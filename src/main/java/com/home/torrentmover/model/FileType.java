package com.home.torrentmover.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NoResultException;
import javax.persistence.Table;

@Entity
@Table(name = "FileType")
public class FileType {

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	private int id;
	@Column(unique = true, nullable = false)
	private String type;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public static FileType findWithName(EntityManager em, String name) throws NoResultException {
		return em.createQuery("SELECT ft FROM FileType ft WHERE ft.type = :typeName", FileType.class)
				.setParameter("typeName", name).getSingleResult();
	}

}
