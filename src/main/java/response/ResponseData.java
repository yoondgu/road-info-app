package response;

public class ResponseData {

	private boolean success;
	private String message;
	private Integer errorCode; // sk API 응답객체의 에러코드. 응답받은 코드가 없을 때(저장하지 않을 때) 0이 아닌 null로 두기 위해 wrapper 클래스로 선언
	
	public static ResponseData create(boolean success, String message) {
		ResponseData responseData = new ResponseData();
		responseData.setSuccess(success);
		responseData.setMessage(message);
		
		return responseData;
	}

	public static ResponseData create(boolean success, String message, int errorCode) {
		ResponseData responseData = new ResponseData();
		responseData.setSuccess(success);
		responseData.setMessage(message);
		responseData.setErrorcode(errorCode);
		
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

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorcode(int errorCode) {
		this.errorCode = errorCode;
	}
	
}
