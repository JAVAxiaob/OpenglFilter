#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 aCoord;
uniform samplerExternalOES vTexture;

void main() {
    vec4 rgba = texture2D(vTexture, aCoord);
    gl_FragColor = rgba;
}