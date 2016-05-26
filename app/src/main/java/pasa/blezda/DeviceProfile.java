package pasa.blezda;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

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



    public static final String[] INPUT_INTENTS = {
            "com.pasa.send"
    };

    public static final String[] OUTPUT_INTENTS = {
            "com.pasa.receive"
    };

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

    public static String encode(Intent intent) {
        StringBuffer sb = new StringBuffer();
        int i;
        for (i = 0; i < DeviceProfile.INPUT_INTENTS.length; i++) {
            if (intent.getAction().equals(DeviceProfile.INPUT_INTENTS[i]))
                break;
        }
        if (i < DeviceProfile.INPUT_INTENTS.length) {
            sb.append("" + i + "&");
            Bundle bundle = intent.getExtras();
            for (String key : bundle.keySet()) {
                Object value = bundle.get(key);
                if (value instanceof String) {
                    sb.append(key + "&");
                    sb.append(value + "&");
                }
            }
        }
        return sb.toString();
    }

    public static Intent decode(String packet) {
        String[] elements = packet.split("&");
        Intent intent = null;
        if (elements.length > 0) {
            int i = 99999;
            try {
                i = Integer.parseInt(elements[0]);
            } catch(NumberFormatException nfe) {
            }
            if (i < DeviceProfile.OUTPUT_INTENTS.length) {
                intent = new Intent(DeviceProfile.OUTPUT_INTENTS[i]);
                int e = 1;
                while (e < elements.length) {
                    String key = elements[e++];
                    if (e < elements.length) {
                        String value = elements[e++];
                        intent.putExtra(key, value);
                    }
                }
            }
        }
        return intent;
    }
}
