/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gestalapidemo;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import okio.ByteString;
import okio.Okio;

public class GestalApiDemo {

    private SSLSocketFactory socketFactory;
    
    private static final HostnameVerifier hostnameVerifier = new HostnameVerifier() {
        public boolean verify(String string, SSLSession ssls) {
            return string.startsWith("https://api.gestal.cloud");
        }
    };
    
    public static void main(String args[]) {
        new GestalApiDemo();
    }

    public GestalApiDemo() {
        try {
            socketFactory = initSocketFactory();
            
            String token = AuthUserPass("user", "pass");
            GetIntegrations(token);
            GetSows("valid_access_key");
            PostSowsMirror("valid_access_key", "valid_sow_mirror_JSON_object");
        }
        catch (Exception e) {
            e.printStackTrace();
        };
    }

    private String AuthUserPass(String user, String pass) throws MalformedURLException,
            IOException, NoSuchAlgorithmException, KeyManagementException {
        String token = null;
        
        URL url = new URL("https://api.gestal.cloud/auth");

        HttpURLConnection connection = initConnection(url, socketFactory);

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Basic "
                + ByteString.encodeUtf8(user + ":" + pass).base64());

        int responseCode = connection.getResponseCode();
        System.out.println("Code "+responseCode);

        if (responseCode >= 200 && responseCode < 300) {
            String tokenObject = new String(
                    connection.getInputStream().readAllBytes());
            System.out.println(tokenObject);

            Gson gson = new Gson();
            token = gson.fromJson(tokenObject, Map.class).get("token").toString();
        }
        else{
            String error = Okio.buffer(Okio.source(
                    connection.getErrorStream())).readUtf8();
            System.out.println(error);
        }
        return token;
    }

    private void GetIntegrations(String token) throws MalformedURLException, IOException {
        URL url = new URL("https://api.gestal.cloud/integrator/integrations");

        HttpURLConnection connection = initConnection(url, socketFactory);

        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + token);

        int responseCode = connection.getResponseCode();
        System.out.println("Code "+responseCode);

        if (responseCode >= 200 && responseCode < 300) {
            String integrations = Okio.buffer(Okio.source(
                    connection.getInputStream())).readUtf8();
            System.out.println(integrations);
        }
        else{
            String error = Okio.buffer(Okio.source(
                    connection.getErrorStream())).readUtf8();
            System.out.println(error);
        }
    }
    
    private void GetSows(String access_key) throws MalformedURLException, IOException {
        URL url = new URL("https://api.gestal.cloud/integration/sows");

        HttpURLConnection connection = initConnection(url, socketFactory);

        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Basic "
                + ByteString.encodeUtf8(access_key).base64());

        int responseCode = connection.getResponseCode();
        System.out.println("Code "+responseCode);

        if (responseCode >= 200 && responseCode < 300) {
            String sows = Okio.buffer(Okio.source(
                    connection.getInputStream())).readUtf8();
            System.out.println(sows);
        }
        else{
            String error = Okio.buffer(Okio.source(
                    connection.getErrorStream())).readUtf8();
            System.out.println(error);
        }
    }
    
    private void PostSowsMirror(String access_key, String query)
            throws MalformedURLException, IOException {
        URL url = new URL("https://api.gestal.cloud/integration/mirrors/sows");

        HttpURLConnection connection = initConnection(url, socketFactory);

        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", String.format(
                "application/json;charset=%s", "UTF-8"));

        connection.setRequestProperty("Authorization", "Basic "
                + ByteString.encodeUtf8(access_key).base64());

        OutputStream output = null;
        try {
            output = connection.getOutputStream();
            output.write(query.getBytes("UTF-8"));
        }
        finally {
            if (output != null) {
                output.close();
            }
        }

        int responseCode = connection.getResponseCode();
        System.out.println("Code "+responseCode);

        if (responseCode >= 200 && responseCode < 300) 
        {
            String sows = Okio.buffer(Okio.source(connection.getInputStream())).readUtf8();
            System.out.println(sows);
        }
        else{
            String error = Okio.buffer(Okio.source(connection.getErrorStream())).readUtf8();
            System.out.println(error);
        }
    }

    private SSLSocketFactory initSocketFactory() throws NoSuchAlgorithmException,
            KeyManagementException {
        final SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, new TrustManager[]{
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[]{};
                }
            }
        }, new SecureRandom());
        return sslContext.getSocketFactory();
    }

    private HttpURLConnection initConnection(URL url, SSLSocketFactory socketFactory)
            throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setConnectTimeout(3000);
        connection.setReadTimeout(8000);
        connection.setUseCaches(false);

        connection.setRequestProperty("Accept-Charset", "UTF-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "gestal-cloud-java/0.1.0");
        
        ((HttpsURLConnection) connection).setSSLSocketFactory(socketFactory);
        ((HttpsURLConnection) connection).setHostnameVerifier(hostnameVerifier);

        return connection;

    }
}
