package com.itpa.mvideo.misc;

import android.os.SystemClock;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class SntpClient {

    private static final int RESPONSE_INDEX_ORIGINATE_TIME = 0;
    private static final int RESPONSE_INDEX_RECEIVE_TIME = 1;
    private static final int RESPONSE_INDEX_TRANSMIT_TIME = 2;
    private static final int RESPONSE_INDEX_RESPONSE_TIME = 3;
    private static final int RESPONSE_INDEX_ROOT_DELAY = 4;
    private static final int RESPONSE_INDEX_DISPERSION = 5;
    private static final int RESPONSE_INDEX_STRATUM = 6;
    private static final int RESPONSE_INDEX_RESPONSE_TICKS = 7;
    private static final int RESPONSE_INDEX_SIZE = 8;

    private static final String TAG = SntpClient.class.getSimpleName();

    private static final int NTP_PORT = 123;
    private static final int NTP_MODE = 3;
    private static final int NTP_VERSION = 3;
    private static final int NTP_PACKET_SIZE = 48;

    private static final int INDEX_VERSION = 0;
    private static final int INDEX_ROOT_DELAY = 4;
    private static final int INDEX_ROOT_DISPERSION = 8;
    private static final int INDEX_ORIGINATE_TIME = 24;
    private static final int INDEX_RECEIVE_TIME = 32;
    private static final int INDEX_TRANSMIT_TIME = 40;

    private static final long OFFSET_1900_TO_1970 = ((365L * 70L) + 17L) * 24L * 60L * 60L;

    //https://en.wikipedia.org/wiki/Network_Time_Protocol#Clock_synchronization_algorithm

    /*private static long getRoundTripDelay(long[] response) {
        return (response[RESPONSE_INDEX_RESPONSE_TIME] - response[RESPONSE_INDEX_ORIGINATE_TIME]) -
                (response[RESPONSE_INDEX_TRANSMIT_TIME] - response[RESPONSE_INDEX_RECEIVE_TIME]);
    };*/

    private static long getClockOffset(long[] response) {
        return ((response[RESPONSE_INDEX_RECEIVE_TIME] - response[RESPONSE_INDEX_ORIGINATE_TIME]) +
                (response[RESPONSE_INDEX_TRANSMIT_TIME] - response[RESPONSE_INDEX_RESPONSE_TIME])) / 2;
    }

    private boolean mInitialized;
    private long mCachedSntpTime = 0, mCachedDeviceUptime = 0;

    SntpClient(String ntpHost, float rootDelayMax, float rootDispersionMax, int serverResponseDelayMax, int timeoutInMillis) {
        DatagramSocket socket = null;
        try {
            byte[] buffer = new byte[NTP_PACKET_SIZE];
            InetAddress address = InetAddress.getByName(ntpHost);
            DatagramPacket request = new DatagramPacket(buffer, buffer.length, address, NTP_PORT);
            writeVersion(buffer);
            long requestTime = System.currentTimeMillis(), requestTicks = SystemClock.elapsedRealtime();
            writeTimeStamp(buffer, INDEX_TRANSMIT_TIME, requestTime);
            socket = new DatagramSocket();
            socket.setSoTimeout(timeoutInMillis);
            socket.send(request);
            long t[] = new long[RESPONSE_INDEX_SIZE];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            socket.receive(response);
            long responseTicks = SystemClock.elapsedRealtime();
            t[RESPONSE_INDEX_RESPONSE_TICKS] = responseTicks;
            long originateTime = readTimeStamp(buffer, INDEX_ORIGINATE_TIME);     // T0
            long receiveTime = readTimeStamp(buffer, INDEX_RECEIVE_TIME);         // T1
            long transmitTime = readTimeStamp(buffer, INDEX_TRANSMIT_TIME);       // T2
            long responseTime = requestTime + (responseTicks - requestTicks);       // T3
            t[RESPONSE_INDEX_ORIGINATE_TIME] = originateTime;
            t[RESPONSE_INDEX_RECEIVE_TIME] = receiveTime;
            t[RESPONSE_INDEX_TRANSMIT_TIME] = transmitTime;
            t[RESPONSE_INDEX_RESPONSE_TIME] = responseTime;
            t[RESPONSE_INDEX_ROOT_DELAY] = read(buffer, INDEX_ROOT_DELAY);
            double rootDelay = doubleMillis(t[RESPONSE_INDEX_ROOT_DELAY]);
            if (rootDelay > rootDelayMax) {
                throw new InvalidNtpServerResponseException(
                        "Invalid response from NTP server. %s violation. %f [actual] > %f [expected]",
                        "root_delay", (float) rootDelay, rootDelayMax);
            }
            t[RESPONSE_INDEX_DISPERSION] = read(buffer, INDEX_ROOT_DISPERSION);
            double rootDispersion = doubleMillis(t[RESPONSE_INDEX_DISPERSION]);
            if (rootDispersion > rootDispersionMax) {
                throw new InvalidNtpServerResponseException(
                        "Invalid response from NTP server. %s violation. %f [actual] > %f [expected]",
                        "root_dispersion", (float) rootDispersion, rootDispersionMax);
            }
            final byte mode = (byte) (buffer[0] & 0x7);
            if (mode != 4 && mode != 5) { throw new InvalidNtpServerResponseException("untrusted mode value for TrueTime: " + mode); }
            final int stratum = buffer[1] & 0xff;
            t[RESPONSE_INDEX_STRATUM] = stratum;
            if (stratum < 1 || stratum > 15) { throw new InvalidNtpServerResponseException("untrusted stratum value for TrueTime: " + stratum); }
            final byte leap = (byte) ((buffer[0] >> 6) & 0x3);
            if (leap == 3) { throw new InvalidNtpServerResponseException("unsynchronized server responded for TrueTime"); }
            double delay = Math.abs((responseTime - originateTime) - (transmitTime - receiveTime));
            if (delay >= serverResponseDelayMax) {
                throw new InvalidNtpServerResponseException(
                        "%s too large for comfort %f [actual] >= %f [expected]",
                        "server_response_delay", (float) delay, serverResponseDelayMax);
            }
            long timeElapsedSinceRequest = Math.abs(originateTime - System.currentTimeMillis());
            if (timeElapsedSinceRequest >= 10_000) {
                throw new InvalidNtpServerResponseException("Request was sent more than 10 seconds back " + timeElapsedSinceRequest);
            }
            Log.i(TAG, "---- SNTP successful response from " + ntpHost);
            cacheTrueTimeInfo(t);
            mInitialized = true;
        } catch (Exception e) {
            Log.d(TAG, "---- SNTP request failed for " + ntpHost);
            mInitialized = false;
        } finally { if (socket != null) { socket.close(); } }
    }

    boolean wasInitialized() { return mInitialized; }
    long getCachedSntpTime() { return mCachedSntpTime; }
    long getCachedDeviceUptime() { return mCachedDeviceUptime; }

    private void cacheTrueTimeInfo(long[] response) { mCachedSntpTime = sntpTime(response); mCachedDeviceUptime = response[RESPONSE_INDEX_RESPONSE_TICKS]; }

    private long sntpTime(long[] response) {
        long clockOffset = getClockOffset(response), responseTime = response[RESPONSE_INDEX_RESPONSE_TIME];
        return responseTime + clockOffset;
    }

    private void writeVersion(byte[] buffer) { buffer[INDEX_VERSION] = NTP_MODE | (NTP_VERSION << 3); }

    private void writeTimeStamp(byte[] buffer, int offset, long time) {
        long seconds = time / 1000L, milliseconds = time - seconds * 1000L, fraction = milliseconds * 0x100000000L / 1000L;
        seconds += OFFSET_1900_TO_1970;
        buffer[offset++] = (byte) (seconds >> 24);
        buffer[offset++] = (byte) (seconds >> 16);
        buffer[offset++] = (byte) (seconds >> 8);
        buffer[offset++] = (byte) (seconds /*>> 0*/);
        buffer[offset++] = (byte) (fraction >> 24);
        buffer[offset++] = (byte) (fraction >> 16);
        buffer[offset++] = (byte) (fraction >> 8);
        buffer[offset/*++*/] = (byte) (Math.random() * 255.0);
    }

    private long readTimeStamp(byte[] buffer, int offset) {
        long seconds = read(buffer, offset), fraction = read(buffer, offset + 4);
        return ((seconds - OFFSET_1900_TO_1970) * 1000) + ((fraction * 1000L) / 0x100000000L);
    }

    private long read(byte[] buffer, int offset) {
        byte b0 = buffer[offset], b1 = buffer[offset + 1], b2 = buffer[offset + 2], b3 = buffer[offset + 3];
        return ((long) ui(b0) << 24) + ((long) ui(b1) << 16) + ((long) ui(b2) << 8) + (long) ui(b3);
    }

    private int ui(byte b) { return b & 0xFF; }
    private double doubleMillis(long fix) { return fix / 65.536D; }
}
