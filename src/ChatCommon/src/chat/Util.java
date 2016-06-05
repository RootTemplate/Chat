package chat;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Util {
    public static final SimpleDateFormat FORMATTER = new SimpleDateFormat("HH:mm:ss");
    
    public static String timestampToString(long timestamp) {
        return FORMATTER.format(new Date(timestamp));
    }
    public static String timeString() {
        return timestampToString(System.currentTimeMillis());
    }
    
    public static int getPropertyUnsignedInt(String propName, int defValue) throws ExitException {
        String prop = System.getProperty(propName);
        if(prop == null) return defValue;
        if(prop.equalsIgnoreCase("INF"))
            return Integer.MAX_VALUE;
        
        int result = Integer.parseInt(prop);
        if(result < 0) {
            System.out.println(propName + " должен быть >= 0");
            throw new ExitException();
        }
        return result;
    }
}
