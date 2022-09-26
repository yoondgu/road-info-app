package dto;

import vo.Coord;

public class MatchedPoint {

	private Coord sourceLocation;
	private String idxName;
	private String linkId;
	private String speed;
	private String roadCategoryName;

	public Coord getSourceLocation() {
		return sourceLocation;
	}

	public String getIdxName() {
		return idxName;
	}

	public String getLinkId() {
		return linkId;
	}

	public String getSpeed() {
		return speed;
	}

	public String getRoadCategoryName() {
		return roadCategoryName;
	}

	public void setSourceLocation(Coord sourceLocation) {
		this.sourceLocation = sourceLocation;
	}

	public void setIdxName(String idxName) {
		this.idxName = idxName;
	}

	public void setLinkId(String linkId) {
		this.linkId = linkId;
	}

	public void setSpeed(String speed) {
		this.speed = speed;
	}

	public void setRoadCategoryName(String roadCategoryName) {
		this.roadCategoryName = roadCategoryName;
	}
	
}
