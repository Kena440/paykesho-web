package org.chaz.membershipsite.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class AmazonS3Util {
    private static final String BUCKET_NAME;
    private static final Logger LOGGER = LoggerFactory.getLogger(AmazonS3Util.class);

    static {
        BUCKET_NAME = System.getenv("AWS_BUCKET_NAME");
    }

    public static List<String> listFolder(String folderName) {
        S3Client s3Client = S3Client.builder().build();
        ListObjectsRequest listObjectsRequest = ListObjectsRequest.builder().bucket(BUCKET_NAME).prefix(folderName).build();

        ListObjectsResponse response = s3Client.listObjects(listObjectsRequest);

        List<S3Object> s3Objects = response.contents();

        ListIterator<S3Object> listIterator = s3Objects.listIterator();

        List<String> listKeys = new ArrayList<>();
        while (listIterator.hasNext()) {
            S3Object object = listIterator.next();
            listKeys.add(object.key());
        }

        return listKeys;
    }

    public static void uploadFile(String folderName, String fileName, InputStream inputStream) {
        S3Client s3Client = S3Client.builder().build();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket(BUCKET_NAME).key(folderName + "/" + fileName).acl("public-read").build();
        try (inputStream) {
            int contentLength = inputStream.available();
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, contentLength));
        } catch (IOException exception) {
            LOGGER.error("Could not upload file to amazon s3 server", exception);
        }
    }

    public static void deleteFile(String fileName) {
        S3Client s3Client = S3Client.builder().build();

        DeleteObjectRequest request = DeleteObjectRequest.builder().bucket(BUCKET_NAME).key(fileName).build();
        s3Client.deleteObject(request);
    }

    public static void removeFolder(String folderName) {
        S3Client s3Client = S3Client.builder().build();

        ListObjectsRequest listObjectsRequest = ListObjectsRequest.builder().bucket(BUCKET_NAME).prefix(folderName).build();

        ListObjectsResponse response = s3Client.listObjects(listObjectsRequest);

        List<S3Object> s3Objects = response.contents();

        ListIterator<S3Object> listIterator = s3Objects.listIterator();

        while (listIterator.hasNext()) {
            S3Object object = listIterator.next();
            DeleteObjectRequest request = DeleteObjectRequest.builder().bucket(BUCKET_NAME).key(object.key()).build();
            s3Client.deleteObject(request);
        }
    }
}
