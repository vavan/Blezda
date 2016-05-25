package pasa.blezda;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

/**
 * Dave Smith
 * Date: 11/13/14
 * DeviceProfile
 * Service/Characteristic constant for our custom peripheral
 */
public class DeviceProfile {

    /* Unique ids generated for this device by 'uuidgen'. Doesn't conform to any SIG profile. */

    //Service UUID to expose our time characteristics
    public static UUID SERVICE_UUID = UUID.fromString("19aa6498-ddf9-4222-884f-49929427ac59");
    //Read-only characteristic providing number of elapsed seconds since offset
    public static UUID CHARACTERISTIC_COMMAND_UUID = UUID.fromString("2fca450b-26ea-4146-96f7-d04725ab20c5");
    //Read-write characteristic for current offset timestamp
//    public static UUID CHARACTERISTIC_OFFSET_UUID = UUID.fromString("9da222a0-e501-4450-bb85-cc72f973ae83");

    //30be097b-e305-43ff-83e4-9531a3319238
    //30b141f3-ab53-4207-bc2b-6fc2489d2c02

    public static String getStateDescription(int state) {
        switch (state) {
            case BluetoothProfile.STATE_CONNECTED:
                return "Connected";
            case BluetoothProfile.STATE_CONNECTING:
                return "Connecting";
            case BluetoothProfile.STATE_DISCONNECTED:
                return "Disconnected";
            case BluetoothProfile.STATE_DISCONNECTING:
                return "Disconnecting";
            default:
                return "Unknown State "+state;
        }
    }

    public static String getStatusDescription(int status) {
        switch (status) {
            case BluetoothGatt.GATT_SUCCESS:
                return "SUCCESS";
            default:
                return "Unknown Status "+status;
        }
    }

    public static byte[] getShiftedTimeValue(int timeOffset) {
        int value = Math.max(0,
                (int)(System.currentTimeMillis()/1000) - timeOffset);
        return bytesFromInt(value);
    }

    public static int unsignedIntFromBytes(byte[] raw) {
        if (raw.length < 4) throw new IllegalArgumentException("Cannot convert raw data to int");

        return ((raw[0] & 0xFF)
                + ((raw[1] & 0xFF) << 8)
                + ((raw[2] & 0xFF) << 16)
                + ((raw[3] & 0xFF) << 24));
    }

    public static byte[] bytesFromInt(int value) {
        //Convert result into raw bytes. GATT APIs expect LE order
        return ByteBuffer.allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(value)
                .array();
    }

    public static byte getCoomand(BluetoothGattCharacteristic characteristic) {
        final int value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
        return (byte)((value & 0xff00) >> 8);
    }
    public static byte getArgument(BluetoothGattCharacteristic characteristic) {
        final int value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
        return (byte)(value & 0xff);
    }
}
