package com.wikicollection.domain.port.out;

public interface ImageHostingClient {

    String upload(byte[] content, String filename, String contentType);

    void delete(String filename);
}
