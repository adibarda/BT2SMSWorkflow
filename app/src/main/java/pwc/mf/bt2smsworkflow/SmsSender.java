package pwc.mf.bt2smsworkflow;

import java.util.ArrayList;

import org.json.JSONObject;
import android.telephony.SmsManager;

public class SmsSender {

    public void sendSms(String phone, String text){
        try {
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(text);
            smsManager.sendMultipartTextMessage(phone, null, parts, null, null);
        } catch(NullPointerException ex) {

        }
    }


}
