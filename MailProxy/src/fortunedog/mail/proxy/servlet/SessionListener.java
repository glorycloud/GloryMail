/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fortunedog.mail.proxy.servlet;

import java.util.HashMap;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fortunedog.mail.proxy.MailClient;
import fortunedog.mail.proxy.checker.MailCheckerManager;
import fortunedog.util.DbHelper;
import fortunedog.util.Utils;

public final class SessionListener implements ServletContextListener,
		HttpSessionAttributeListener, HttpSessionListener
{
	public static final String ATTR_MAIL_CLIENT = "mailClient";
	static Logger log = LoggerFactory.getLogger(SessionListener.class);
	private static HashMap<String, HttpSession> sessions = new HashMap<String, HttpSession>();

	public static HttpSession getSession(String sessionId)
	{
		return sessions.get(sessionId);
	}
	public static HttpSession removeSession(String sessionId)
	{
		return sessions.remove(sessionId);
	}



	/**
	 * Record the fact that a servlet context attribute was added.
	 * 
	 * @param event
	 *            The session attribute event
	 */
	public void attributeAdded(HttpSessionBindingEvent event)
	{
		log("attributeAdded('" + event.getSession().getId() + "', '"
				+ event.getName() + "', '" + event.getValue() + "')");
	}

	/**
	 * Record the fact that a servlet context attribute was removed.
	 * 
	 * @param event
	 *            The session attribute event
	 */
	public void attributeRemoved(HttpSessionBindingEvent event)
	{
		log("attributeRemoved('" + event.getSession().getId() + "', '"
				+ event.getName() + "', '" + event.getValue() + "')");
	}

	/**
	 * Record the fact that a servlet context attribute was replaced.
	 * 
	 * @param event
	 *            The session attribute event
	 */
	public void attributeReplaced(HttpSessionBindingEvent event)
	{
		log("attributeReplaced('" + event.getSession().getId() + "', '"
				+ event.getName() + "', '" + event.getValue() + "')");
	}

	/**
	 * Record the fact that this web application has been destroyed.
	 * 
	 * @param event
	 *            The servlet context event
	 */
	public void contextDestroyed(ServletContextEvent event)
	{
		log("contextDestroyed()");

	}

	/**
	 * Record the fact that this web application has been initialized.
	 * 
	 * @param event
	 *            The servlet context event
	 */
	public void contextInitialized(ServletContextEvent event)
	{
		log("contextInitialized()");

	}

	/**
	 * Record the fact that a session has been created.
	 * 
	 * @param event
	 *            The session event
	 */
	public void sessionCreated(HttpSessionEvent event)
	{
		log("sessionCreated('" + event.getSession().getId() + "')");
		sessions.put(event.getSession().getId(), event.getSession());
	}

	/**
	 * Record the fact that a session has been destroyed.
	 * 
	 * @param event
	 *            The session event
	 */
	public void sessionDestroyed(HttpSessionEvent event)
	{
		log("sessionDestroyed('" + event.getSession().getId() + "')");
		HttpSession s = event.getSession();
		sessions.remove(event.getSession().getId());
		MailCheckerManager.stopPush(s);
		MailClient c = SessionListener.getStoredMailClient(s);
		if(c != null)
			c.decRef();
		DbHelper.closeConnectionForAccount(c.connData.accountId);

	}


	/**
	 * Log a message to the servlet context application log.
	 * 
	 * @param message
	 *            Message to be logged
	 */
	private void log(String message)
	{
		log.info( message);
	}
	public static void removeStoredMailClient(HttpSession session)
	{
		try
		{
			session.removeAttribute("mailClient");
		}
		catch(IllegalStateException ie)
		{
			removeSession(session.getId());
			log.trace("removeStoredMailClient", ie);
		}
		
	}
	public static MailClient getStoredMailClient(HttpSession session)
	{
		try
		{
			return (MailClient) session.getAttribute("mailClient");
		}
		catch(IllegalStateException ie)
		{
			removeSession(session.getId());
			log.trace("getStoredMailClient", ie);
			return null;
		}
	}

}
