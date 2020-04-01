package com.home.torrentmover;

import java.io.IOException;

import javax.persistence.EntityManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.home.torrentmover.model.SubscriptionDTO;

public class HomepageHandler extends AbstractHandler {
	private static final Logger LOG = LoggerFactory.getLogger(HomepageHandler.class);

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		response.setStatus(HttpServletResponse.SC_OK);
		LOG.info("Target :" + target);

		final String fileToSend;

		switch (target) {
		case "/homepage.css":
			fileToSend = "homepage.css";
			response.setContentType("text/css");
			break;
		case "/favicon.ico":
			fileToSend = "home-solid.svg";
			response.setContentType("image/svg+xml");
			break;
		case "/homepage.js":
			fileToSend = "homepage.js";
			response.setContentType("text/javascript");
			break;
		case "/sw.js":
			fileToSend = "sw.js";
			response.setContentType("text/javascript");
			break;
		case "/rest/saveSub":
			saveSubscription(request);
			fileToSend = "success.json";
			response.setContentType("text/json");
			break;
		default:
			fileToSend = "homepage.html";
			response.setContentType("text/html; charset=utf-8");
			break;
		}

		LOG.info("Response File :" + fileToSend);
		
		IOUtils.copy(this.getClass().getResourceAsStream(fileToSend), response.getOutputStream());

		baseRequest.setHandled(true);
	}
	
	private void saveSubscription(HttpServletRequest req) {
		final SubscriptionDTO subDTO = new SubscriptionDTO();
		subDTO.setEndpoint(req.getParameter("endpoint"));
		subDTO.setP256dh(req.getParameter("p256dh"));
		subDTO.setAuth(req.getParameter("auth"));
		final EntityManager em = SpringStart.getEm();
		if (em != null) {
			em.getTransaction().begin();
			em.persist(subDTO);
			em.getTransaction().commit();
		}
	}
}
