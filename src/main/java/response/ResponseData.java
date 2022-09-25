package response;

public class ResponseData {

	private boolean success;
	private String message;
	
	public static ResponseData create(boolean success, String message) {
		ResponseData responseData = new ResponseData();
		responseData.setSuccess(success);
		responseData.setMessage(message);
		
		return responseData;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
}
