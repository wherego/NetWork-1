package com.android.network.okhttp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import com.android.network.okhttp.utils.ImageUtils;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.lang.reflect.ParameterizedType;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.Map;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 *
 * Created by kid on 2016/11/24.
 */

public class OkHttpClientManager {
    private static OkHttpClientManager mInstance;
    private OkHttpClient mOkHttpClient;
    private Handler mDelivery;
    private Gson mGson;

    private static final String TAG = "OkHttpClientManager";

    private OkHttpClientManager()
    {
        mOkHttpClient = new OkHttpClient();
        //cookie enabled
        mDelivery = new Handler(Looper.getMainLooper());
        mGson = new Gson();
    }

    public static OkHttpClientManager getInstance()
    {
        if (mInstance == null)
        {
            synchronized (OkHttpClientManager.class)
            {
                if (mInstance == null)
                {
                    mInstance = new OkHttpClientManager();
                }
            }
        }
        return mInstance;
    }

    /**
     * 同步的Get请求
     *
     * @param url
     * @return Response
     */
    public Response getAsync(String url) throws IOException
    {
        final Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = mOkHttpClient.newCall(request);
        Response execute = call.execute();
        return execute;
    }

    /**
     * 同步的Get请求
     *
     * @param url
     * @return 字符串
     */
    public String getAsString(String url) throws IOException
    {
        Response execute = getAsync(url);
        return execute.body().string();
    }


    /**
     * 异步的get请求
     *
     * @param url
     * @param callback
     */
    public void getAsync(String url, final ResultCallback callback)
    {
        final Request request = new Request.Builder()
                .url(url)
                .build();
        deliveryResult(callback, request);
    }

    /**
     * 同步的Post请求
     *
     * @param url
     * @param params post的参数
     * @return
     */
    public Response postForResponse(String url, Param... params) throws IOException
    {
        Request request = buildPostRequest(url, params);
        Response response = mOkHttpClient.newCall(request).execute();
        return response;
    }

    /**
     * 同步的Post请求
     *
     * @param url
     * @param params post的参数
     * @return
     */
    public String postForString(String url, Param... params) throws IOException
    {
        Response response = postForResponse(url , params);
        return response.body().toString();
    }

    /**
     * 异步的post请求
     *
     * @param url
     * @param callback
     * @param params
     */
    public void postAsyn(String url, final ResultCallback callback, Param... params)
    {
        Request request = buildPostRequest(url, params);
        deliveryResult(callback, request);
    }

    /**
     * 异步的post请求
     *
     * @param url
     * @param callback
     * @param params
     */
    public void postAsyn(String url, final ResultCallback callback, Map<String , String> params)
    {
        Param[] param = map2Params(params);
        Request request = buildPostRequest(url, param);
        deliveryResult(callback, request);
    }

    /**
     * 同步基于post的文件上传
     *
     * @param params
     * @return
     */
    public Response post(String url, File[] files, String[] fileKeys, Param... params) throws IOException
    {
        Request request = buildMultipartFormRequest(url, files, fileKeys, params);

        return mOkHttpClient.newCall(request).execute();
    }

    public Response post(String url, File file, String fileKey) throws IOException
    {
        Request request = buildMultipartFormRequest(url, new File[]{file}, new String[]{fileKey}, null);
        return mOkHttpClient.newCall(request).execute();
    }

    public Response post(String url, File file, String fileKey, Param... params) throws IOException
    {
        Request request = buildMultipartFormRequest(url, new File[]{file}, new String[]{fileKey}, params);
        return mOkHttpClient.newCall(request).execute();
    }

    /**
     * 异步基于post的文件上传
     *
     * @param url
     * @param callback
     * @param files
     * @param fileKeys
     * @throws IOException
     */
    public void postAsyn(String url, ResultCallback callback, File[] files, String[] fileKeys, Param... params) throws IOException
    {
        Request request = buildMultipartFormRequest(url, files, fileKeys, params);
        deliveryResult(callback, request);
    }

    /**
     * 异步基于post的文件上传，单文件不带参数上传
     *
     * @param url
     * @param callback
     * @param file
     * @param fileKey
     * @throws IOException
     */
    public void postAsyn(String url, ResultCallback callback, File file, String fileKey) throws IOException
    {
        Request request = buildMultipartFormRequest(url, new File[]{file}, new String[]{fileKey}, null);
        deliveryResult(callback, request);
    }

    /**
     * 异步下载文件
     *
     * @param url
     * @param destFileDir 本地文件存储的文件夹
     * @param callback
     */
    public void downloadAsyn(final String url, final String destFileDir, final ResultCallback callback)
    {
        final Request request = new Request.Builder()
                .url(url)
                .build();
        final Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                sendFailedStringCallback(call.request() , e , callback);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream inputStream = null;
                byte[] buf = new byte[2048];
                int len = 0;
                FileOutputStream fileOutputStream = null ;
                try{
                    inputStream = response.body().byteStream();
                    File file = new File(destFileDir , getFileName(url));
                    fileOutputStream = new FileOutputStream(file);
                    while((len = inputStream.read())!= -1){
                        fileOutputStream.write(buf , 0 , len);
                    }
                    fileOutputStream.close();
                    sendSuccessResultCallback(file.getAbsolutePath() , callback);
                }catch (IOException e){
                    sendFailedStringCallback(request , e , callback);
                }finally {
                    try{
                        if (inputStream != null){
                            inputStream.close();
                        }
                    }catch (IOException e){

                    }
                    try{
                        if (fileOutputStream != null){
                            fileOutputStream.close();
                        }
                    }catch (IOException e){

                    }
                }
            }
        });
    }

    /**
     * 加载图片
     *
     * @param view
     * @param url
     * @throws IOException
     */
    public void displayImage(final ImageView view, final String url, final int errorResId)
    {
        final Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e) {
                setErrorResId(view, errorResId);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream is = null;
                try
                {
                    is = response.body().byteStream();
                    ImageUtils.ImageSize actualImageSize = ImageUtils.getImageSize(is);
                    ImageUtils.ImageSize imageViewSize = ImageUtils.getImageViewSize(view);
                    int inSampleSize = ImageUtils.calculateInSampleSize(actualImageSize, imageViewSize);
                    try
                    {
                        is.reset();
                    } catch (IOException e)
                    {
                        response = getAsync(url);
                        is = response.body().byteStream();
                    }

                    BitmapFactory.Options ops = new BitmapFactory.Options();
                    ops.inJustDecodeBounds = false;
                    ops.inSampleSize = inSampleSize;
                    final Bitmap bm = BitmapFactory.decodeStream(is, null, ops);
                    mDelivery.post(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            view.setImageBitmap(bm);
                        }
                    });
                } catch (Exception e)
                {
                    setErrorResId(view, errorResId);

                } finally
                {
                    if (is != null) try
                    {
                        is.close();
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }

        });
    }

    /*=================================================================================================================*/



    /*=================================================================================================================*/

    /**
     * 组建Post请求体
     * @param url
     * @param params
     * @return
     */
    private Request buildPostRequest(String url, Param[] params)
    {
        if (params == null)
        {
            params = new Param[0];
        }
        FormBody.Builder builder = new FormBody.Builder();
        for (Param param : params)
        {
            builder.add(param.key, param.value);
        }
        RequestBody requestBody = builder.build();
        return new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
    }

    private String getFileName(String path)
    {
        int separatorIndex = path.lastIndexOf("/");
        return (separatorIndex < 0) ? path : path.substring(separatorIndex + 1, path.length());
    }

    private void setErrorResId(final ImageView view, final int errorResId)
    {
        mDelivery.post(new Runnable()
        {
            @Override
            public void run()
            {
                view.setImageResource(errorResId);
            }
        });
    }

    /**
     * build form through params
     * @param url
     * @param files
     * @param fileKeys
     * @param params
     * @return
     */
    private Request buildMultipartFormRequest(String url, File[] files,
                                              String[] fileKeys, Param[] params)
    {
        params = validateParam(params);

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        for (Param param : params)
        {
            builder.addPart(Headers.of("Content-Disposition", "form-data; name=\"" + param.key + "\""),
                    RequestBody.create(null, param.value));
        }
        if (files != null)
        {
            RequestBody fileBody = null;
            for (int i = 0; i < files.length; i++)
            {
                File file = files[i];
                String fileName = file.getName();
                fileBody = RequestBody.create(MediaType.parse(guessMimeType(fileName)), file);
                //TODO 根据文件名设置contentType
                builder.addPart(Headers.of("Content-Disposition",
                        "form-data; name=\"" + fileKeys[i] + "\"; filename=\"" + fileName + "\""),
                        fileBody);
            }
        }

        RequestBody requestBody = builder.build();
        return new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
    }

    /**
     * judge content Type by  file-path
     * @param path
     * @return
     */
    private String guessMimeType(String path)
    {
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        String contentTypeFor = fileNameMap.getContentTypeFor(path);
        if (contentTypeFor == null)
        {
            contentTypeFor = "application/octet-stream";
        }
        return contentTypeFor;
    }


    /**
     * 派发请求结果
     * @param callback
     * @param request
     */
    private void deliveryResult(final ResultCallback callback, Request request)
    {
        mOkHttpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e) {
                sendFailedStringCallback(call.request(), e, callback);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try
                {
                    final String string = response.body().string();
                    if (callback.mType == String.class)
                    {
                        sendSuccessResultCallback(string, callback);
                    } else
                    {
                        Object o = mGson.fromJson(string, callback.mType);
                        sendSuccessResultCallback(o, callback);
                    }


                } catch (IOException e)
                {
                    sendFailedStringCallback(response.request(), e, callback);
                } catch (com.google.gson.JsonParseException e)//Json解析的错误
                {
                    sendFailedStringCallback(response.request(), e, callback);
                }
            }
        });
    }

    /**
     * 回调失败结果
     * @param request
     * @param e
     * @param callback
     */
    private void sendFailedStringCallback(final Request request, final Exception e, final ResultCallback callback)
    {
        mDelivery.post(new Runnable()
        {
            @Override
            public void run()
            {
                if (callback != null)
                    callback.onError(request, e);
            }
        });
    }

    /**
     * 回调成功结果
     * @param object
     * @param callback
     */
    private void sendSuccessResultCallback(final Object object, final ResultCallback callback)
    {
        mDelivery.post(new Runnable()
        {
            @Override
            public void run()
            {
                if (callback != null)
                {
                    callback.onResponse(object);
                }
            }
        });
    }

    private Param[] validateParam(Param[] params)
    {
        if (params == null)
            return new Param[0];
        else return params;
    }

    /**
     * 将键值对类型参数化为可变参数
     * @param params
     * @return
     */
    private Param[] map2Params(Map<String, String> params)
    {
        if (params == null) return new Param[0];
        int size = params.size();
        Param[] res = new Param[size];
        Set<Map.Entry<String, String>> entries = params.entrySet();
        int i = 0;
        for (Map.Entry<String, String> entry : entries)
        {
            res[i++] = new Param(entry.getKey(), entry.getValue());
        }
        return res;
    }

}
