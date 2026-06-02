package br.com.ottonsam.toothy_planner_api.user.usecases;

public final class ProfileImageData {

    private final byte[] content;
    private final String contentType;

    public ProfileImageData(byte[] content, String contentType) {
        this.content = content.clone();
        this.contentType = contentType;
    }

    public byte[] content() {
        return content.clone();
    }

    public String contentType() {
        return contentType;
    }
}
