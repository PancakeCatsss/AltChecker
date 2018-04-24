package bobcatsss.altchecker;

import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import simple.brainsynder.nbt.StorageTagCompound;
import simple.brainsynder.utils.Base64Wrapper;

import java.util.*;

/**
 * Data to store:
 * - IPs (A list of all last known IPs) [ENCRYPTED]
 * - Last IP (Last IP they used to log in with) [ENCRYPTED]
 * - UUID (Will be the "KEY" to get the data)
 * - Username
 */
public class UserData {

    /* STATIC */
    private static Map<String, UserData> userDataMap = new HashMap<>();
    public static UserData getData(String uuid) {
        if (userDataMap.containsKey(uuid)) return userDataMap.get(uuid);
        UserData data = new UserData();
        userDataMap.put(uuid, data);
        return data;
    }
    public static Collection<UserData> collectData () {
        return userDataMap.values();
    }

    private StorageTagCompound compound = new StorageTagCompound();
    private String name = "", uuid = "", lastKnownIP = "";
    private List<String> knownIps = new ArrayList<>();


    /* SERIALIZATION */
    public void fromCompound(StorageTagCompound compound) {
        this.compound = compound;
        if (compound.hasKey("name")) name = compound.getString("name");
        if (compound.hasKey("uuid")) uuid = compound.getString("uuid");
        if (compound.hasKey("lastIP")) lastKnownIP = Encrypter.getDecrypted(compound.getString("lastIP"));
        if (compound.hasKey("ips")) {
            try {
                JSONArray array = (JSONArray) JSONValue.parseWithException(Encrypter.getDecrypted(Base64Wrapper.decodeString(compound.getString("ips"))));
                array.forEach(o -> knownIps.add(String.valueOf(o)));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }
    public StorageTagCompound toCompound() {
        try {
            StorageTagCompound compound = new StorageTagCompound();
            compound.setString("name", name);
            compound.setString("uuid", uuid);
            compound.setString("lastIP", Encrypter.getEncrypted(lastKnownIP));
            JSONArray array = new JSONArray();
            array.addAll(knownIps);
            compound.setString("ips", Encrypter.getEncrypted(Base64Wrapper.encodeString(array.toJSONString())));
            return compound;
        }catch (Exception e){
            return compound;
        }
    }

    /* GETTERS */
    public List<String> getKnownIps() {
        return knownIps;
    }
    public String getLastKnownIP() {
        return lastKnownIP;
    }
    public String getName() {
        return name;
    }
    public String getUuid() {
        return uuid;
    }


    /* SETTERS */
    public void setLastKnownIP(String lastKnownIP) {
        if (!knownIps.contains(lastKnownIP)) knownIps.add(lastKnownIP);
        this.lastKnownIP = lastKnownIP;
        compound.setString("lastIP", Encrypter.getEncrypted(lastKnownIP));
        JSONArray array = new JSONArray();
        array.addAll(knownIps);
        compound.setString("ips", Encrypter.getEncrypted(Base64Wrapper.encodeString(array.toJSONString())));

    }
    public void setName(String name) {
        this.name = name;
        compound.setString("name", name);
    }
    public void setUuid(String uuid) {
        this.uuid = uuid;
        compound.setString("uuid", uuid);
    }
}
