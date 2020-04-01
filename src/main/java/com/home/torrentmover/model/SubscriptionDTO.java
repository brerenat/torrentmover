package com.home.torrentmover.model;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NoResultException;
import javax.persistence.Table;

@Entity
@Table(name = "SubscriptionDTO")
public class SubscriptionDTO {
	
	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	private int id = 0;
	@Column(unique = false, nullable = false)
	private String endpoint;
	@Column(unique = false, nullable = false)
	private String p256dh;
	@Column(unique = false, nullable = false)
	private String auth;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public String getP256dh() {
		return p256dh;
	}

	public void setP256dh(String p256dh) {
		this.p256dh = p256dh;
	}

	public String getAuth() {
		return auth;
	}

	public void setAuth(String auth) {
		this.auth = auth;
	}
	
	public static List<SubscriptionDTO> getSubscriptions(EntityManager em) throws NoResultException {
		return em.createQuery("SELECT s FROM SubscriptionDTO s", SubscriptionDTO.class).getResultList();
	}

}
