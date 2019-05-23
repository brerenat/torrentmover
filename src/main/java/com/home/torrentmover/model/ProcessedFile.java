package com.home.torrentmover.model;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NoResultException;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "ProcessedFile")
public class ProcessedFile {

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	private int id;
	@Column(unique = true, nullable = false)
	private String fileName;
	@Temporal(value = TemporalType.TIMESTAMP)
	private Date dateProcessed;
	@ManyToOne(cascade = CascadeType.REMOVE)
	@JoinColumn(name = "fileTypeId", referencedColumnName = "id", foreignKey = @ForeignKey(name = "FK_FileType"))
	private FileType fileType;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public Date getDateProcessed() {
		return dateProcessed;
	}

	public void setDateProcessed(Date dateProcessed) {
		this.dateProcessed = dateProcessed;
	}

	public FileType getFileType() {
		return fileType;
	}

	public void setFileType(FileType fileType) {
		this.fileType = fileType;
	}

	public static ProcessedFile findWithName(EntityManager em, String name) throws NoResultException {
		return em.createQuery("SELECT pf FROM ProcessedFile pf WHERE pf.fileName = :fileName", ProcessedFile.class)
				.setParameter("fileName", name).getSingleResult();
	}

}
