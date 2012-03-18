package fi.bitrite.android.ws.host.impl;

import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CircularRedirectException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import fi.bitrite.android.ws.auth.http.HttpAuthenticationFailedException;
import fi.bitrite.android.ws.auth.http.HttpAuthenticationService;
import fi.bitrite.android.ws.auth.http.HttpSessionContainer;
import fi.bitrite.android.ws.util.http.HttpUtils;

public class HttpHostContact extends HttpPageReader {

	private String contactFormUrl;
	
	public HttpHostContact(HttpAuthenticationService authenticationService, HttpSessionContainer sessionContainer) {
		super(authenticationService, sessionContainer);
	}

	public void send(int id, String subject, String message, boolean copy) {
		contactFormUrl = new StringBuilder().append("http://www.warmshowers.org/user/").append(id)
				.append("/contact").toString();
		send(subject, message, copy);
	}
	
	private void send(String subject, String message, boolean copy) {
		String html = getPage(contactFormUrl);
		List<NameValuePair> formDetails = new HttpHostContactFormScraper(html).getFormDetails();
		formDetails.add(new BasicNameValuePair("subject", subject));
		formDetails.add(new BasicNameValuePair("message", message));
		if (copy) {
			formDetails.add(new BasicNameValuePair("copy", "1"));
		}
		sendMessageForm(formDetails);
	}

	public void send(String name, String subject, String message, boolean copy) {
		contactFormUrl = new StringBuilder().append("http://www.warmshowers.org/users/").append(name)
				.append("/contact").toString();
		send(subject, message, copy);
	}

	private void sendMessageForm(List<NameValuePair> formDetails) {
		HttpClient client = HttpUtils.getDefaultClient();
		try {
			String url = HttpUtils.encodeUrl(contactFormUrl);

			HttpPost post = new HttpPost(url);
			post.setEntity(new UrlEncodedFormEntity(formDetails));
			HttpContext context = sessionContainer.getSessionContext();
			HttpResponse response = client.execute(post, context);
			HttpEntity entity = response.getEntity();

			// Consume response content
			EntityUtils.toString(entity);
		}

		catch (ClientProtocolException e) {
			if (e.getCause() instanceof CircularRedirectException) {
				// If we get this the message has still been sent successfully, so ignore it
			} else {
				throw new HttpAuthenticationFailedException(e);
			}
		}
		
		catch (Exception e) {
			throw new HttpAuthenticationFailedException(e);
		}

		finally {
			client.getConnectionManager().shutdown();
		}
	}
}