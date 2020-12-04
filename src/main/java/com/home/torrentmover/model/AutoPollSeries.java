package com.home.torrentmover.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "AutoPollSeries")
public class AutoPollSeries {

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	private int id = 0;
	@Column(unique = true, nullable = false)
	private String imdbID;
	@Column(unique = false, nullable = false)
	private String posterUrl;
	@Column(unique = false, nullable = false)
	private String title;
	@Column(unique = false, nullable = false)
	private String year;
	@Column(unique = false, nullable = false)
	private boolean active;
	@Column(unique = true, nullable = false)
	private String folderName;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getImdbID() {
		return imdbID;
	}

	public void setImdbID(String imdbID) {
		this.imdbID = imdbID;
	}

	public String getPosterUrl() {
		return posterUrl;
	}

	public void setPosterUrl(String posterUrl) {
		this.posterUrl = posterUrl;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getYear() {
		return year;
	}

	public void setYear(String year) {
		this.year = year;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getFolderName() {
		return folderName;
	}

	public void setFolderName(String folderName) {
		this.folderName = folderName;
	}

}
