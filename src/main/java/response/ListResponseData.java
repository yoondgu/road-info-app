package response;

import java.util.List;

public class ListResponseData<T> extends ResponseData {

	private List<T> items;
	
	public static <T> ListResponseData<T> create(List<T> items) {
		ListResponseData<T> responseData = new ListResponseData<>();
		responseData.setSuccess(true);
//		responseData.setCode(200);
		responseData.setItems(items);
		
		return responseData;
	}

	public List<T> getItems() {
		return items;
	}

	public void setItems(List<T> items) {
		this.items = items;
	}
	
}
