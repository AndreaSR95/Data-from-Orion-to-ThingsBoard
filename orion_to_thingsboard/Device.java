package orion_to_thingsboard;

public class Device {
	
	private String type;
	private String thingsboardId;
	private String thingsboardAccessToken;
	private String id;
	
	public Device(String type, String thingsboardId, String thingsboardAccessToken, String id) {
		this.type = type;
		this.thingsboardId = thingsboardId;
		this.thingsboardAccessToken = thingsboardAccessToken;
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getThingsboardId() {
		return thingsboardId;
	}

	public void setThingsboardId(String thingsboardId) {
		this.thingsboardId = thingsboardId;
	}

	public String getThingsboardAccessToken() {
		return thingsboardAccessToken;
	}

	public void setThingsboardAccessToken(String thingsboardAccessToken) {
		this.thingsboardAccessToken = thingsboardAccessToken;
	}

	public String getId() {
		return this.id;
	}

	public void setId(String thingsboardAccessToken) {
		this.thingsboardAccessToken = this.id;
	}

}
