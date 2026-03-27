package com.maniu.openglfilter;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.opengl.EGLContext;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MediaRecorder {
    private MediaCodec mMediaCodec;
    private int mWidth;
    private int mHeight;
    private String mPath;
    private Surface mSurface;
    private Handler mHandler;
    private MediaMuxer mMuxer;
    private EGLContext mGlContext;
    private EGLEnv eglEnv;
    private boolean isStart;
    private Context mContext;

    private long mLastTimeStamp;
    private int track = -1;
    private float mSpeed;
    private boolean isEncoding = false;

    public MediaRecorder(Context context, String path, EGLContext glContext, int width, int height) {
        mContext = context.getApplicationContext();
        mPath = path;
        mWidth = width;
        mHeight = height;
        mGlContext = glContext;
    }

    public void start(float speed) throws IOException {
        mSpeed = speed;
        track = -1;
        mLastTimeStamp = 0;

        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, mWidth, mHeight);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 1500_000);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 25);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);

        mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mSurface = mMediaCodec.createInputSurface();
        mMuxer = new MediaMuxer(mPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        mMediaCodec.start();
        isEncoding = true;

        HandlerThread handlerThread = new HandlerThread("codec-gl");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                eglEnv = new EGLEnv(mContext, mGlContext, mSurface, mWidth, mHeight);
                isStart = true;
            }
        });
    }

    public void fireFrame(final int textureId, final long timestamp) {
        if (!isStart || !isEncoding) {
            return;
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                eglEnv.draw(textureId, timestamp);
                codec(false);
            }
        });
    }

    private void codec(boolean endOfStream) {
        if (mMediaCodec == null || !isEncoding) {
            return;
        }

        try {
            while (isEncoding) {
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                int index;

                try {
                    index = mMediaCodec.dequeueOutputBuffer(bufferInfo, 1000);
                } catch (IllegalStateException e) {
                    break;
                }

                if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (!endOfStream) {
                        break;
                    } else {
                        continue;
                    }
                } else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat newFormat = mMediaCodec.getOutputFormat();
                    if (mMuxer != null && track < 0) {
                        track = mMuxer.addTrack(newFormat);
                        mMuxer.start();
                    }
                } else if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {

                } else if (index >= 0) {
                    bufferInfo.presentationTimeUs = (long) (bufferInfo.presentationTimeUs / mSpeed);
                    if (bufferInfo.presentationTimeUs <= mLastTimeStamp) {
                        bufferInfo.presentationTimeUs = mLastTimeStamp + 1000000 / 25;
                    }
                    mLastTimeStamp = bufferInfo.presentationTimeUs;

                    ByteBuffer encodedData = mMediaCodec.getOutputBuffer(index);

                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        bufferInfo.size = 0;
                    }

                    if (bufferInfo.size > 0 && mMuxer != null && track >= 0) {
                        encodedData.position(bufferInfo.offset);
                        encodedData.limit(bufferInfo.offset + bufferInfo.size);
                        mMuxer.writeSampleData(track, encodedData, bufferInfo);
                    }

                    mMediaCodec.releaseOutputBuffer(index, false);

                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        isStart = false;
        isEncoding = false;

        if (mHandler != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        codec(true);

                        if (mMediaCodec != null) {
                            mMediaCodec.stop();
                            mMediaCodec.release();
                            mMediaCodec = null;
                        }

                        if (mMuxer != null) {
                            mMuxer.stop();
                            mMuxer.release();
                            mMuxer = null;
                        }

                        if (eglEnv != null) {
                            eglEnv.release();
                            eglEnv = null;
                        }

                        mSurface = null;
                        mHandler.getLooper().quitSafely();
                        mHandler = null;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}