package tk.djcrazy.MyCC98;

import java.io.IOException;
import java.util.List;

import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.cookie.Cookie;

import tk.djcrazy.MyCC98.dialog.MoreEmotChooseDialog;
import tk.djcrazy.MyCC98.dialog.MoreEmotChooseDialog.FaceExpressionChooseListener;
import tk.djcrazy.MyCC98.helper.HtmlGenHelper;
import tk.djcrazy.MyCC98.view.HeaderView;
import tk.djcrazy.libCC98.CC98Client;
import tk.djcrazy.libCC98.CC98Parser;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.flurry.android.FlurryAgent;
/**
 * 
 * @author zsy
 *
 */
public class PmViewActivity extends Activity {
	private static String TAG = "PmReply";
	private WebView webView;

	private String pageString = null;
	private String readTopic = null;
	private String sender = null;
	private String senderAvatarUrl = null;
	private String sendTime = null;
	private String faceChoosedString = null;
	private String pmContent;

	private List<Cookie> cookies;
	private Cookie pmCookie;
	private HeaderView headerView;

	@Override
	public void onStart() {
		super.onStart();
		FlurryAgent.onStartSession(this, "5EXV7SIGMTTDKYNXTKR4");
	}

	@Override
	public void onStop() {
		super.onStop();
		FlurryAgent.onEndSession(this);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		// Get Html header
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		Intent intent = getIntent();

		int pmId = intent.getIntExtra("PmId", -1);
		readTopic = intent.getStringExtra("Topic");
		sender = intent.getStringExtra("Sender");
		sendTime = intent.getStringExtra("SendTime");

		setContentView(R.layout.pm_reply);
		if (pmId == -1) {
			setTitle("新消息");
		} else {
			setTitle(R.string.pm_reply);
		}

		findViews();

		setViews();

		setListeners();
		// pageOpen = this.getString(R.string.pm_reply_html_header);
		preparePage(pmId);
	}

	private void findViews() {

		if ((webView = (WebView) findViewById(R.id.pm_reply_view)) == null) {
			Log.e(TAG, "webView load fail.");
		}

		headerView = (HeaderView) findViewById(R.id.pm_reply_header);
	}

	/**
	 * 
	 */
	private void setViews() {
		// gets all cookies from the HttpClient's cookie jar
		cookies = CC98Client.getCookies();
		headerView.setUserImg(CC98Client.getLoginUserImg());
		headerView.setTitle("查看短消息");
		headerView.setUserImg(CC98Client.getLoginUserImg());
		headerView.setButtonImageResource(R.drawable.pm_reply);
		if (!cookies.isEmpty()) {

			CookieSyncManager.createInstance(PmViewActivity.this);
			CookieManager cookieManager = CookieManager.getInstance();

			// sync all the cookies in the httpclient with the webview by
			// generating cookie string
			for (Cookie cookie : cookies) {

				pmCookie = cookie;

				String cookieString = pmCookie.getName() + "="
						+ pmCookie.getValue() + "; domain="
						+ pmCookie.getDomain();
				cookieManager.setCookie(CC98Client.getCC98Domain(),
						cookieString);
				CookieSyncManager.getInstance().sync();
			}
		}
		WebSettings webSettings = webView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setPluginsEnabled(true);
		webSettings.setLayoutAlgorithm(LayoutAlgorithm.SINGLE_COLUMN);
		webSettings.setAppCacheEnabled(true);
		webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
		webView.setWebViewClient(new WebViewClient() {

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(url));
				startActivity(i);
				return true;
			}
		});
		webView.addJavascriptInterface(this, "PmReply");
		webView.setBackgroundColor(Color.parseColor("#f0f0f0"));
	}

	// handle the message
	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				webView.loadDataWithBaseURL(null, pageString, "text/html",
						"utf-8", null);
//				Log.d("WebView", pageString);
				break;
			case 1:
				webView.loadUrl("javascript:addEmot('" + faceChoosedString
						+ "');");
			default:
				break;
			}
		}
	};

	private void preparePage(final int pmId) {

		new Thread() {
			@Override
			public void run() {
				String replyed = "";
				if (pmId != -1) { // in reply mod
					try {
						pmContent = CC98Parser.getMsgContent(pmId);
						senderAvatarUrl = CC98Client.getUserImgUrl(sender);
						replyed = HtmlGenHelper.addPostInfo(readTopic,
								senderAvatarUrl, sender, "", -1, sendTime, -1)
								+ "<div class=\"post-content\"><span id=\"ubbcode\">"
								+ HtmlGenHelper
										.parseInnerLink(pmContent, "PmReply")
								+ "</span><script>searchubb('ubbcode',1,'tablebody2');</script></div>";
					} catch (ClientProtocolException e) {

						e.printStackTrace();
					} catch (ParseException e) {

						e.printStackTrace();
					} catch (IOException e) {

						e.printStackTrace();
					}
				} else {
					// replyed = "<br /><br /><br />";
					replyed = "";
					if (sender == null) {
						sender = "";
					}
					if (readTopic == null) {
						readTopic = "";
					}
				}
				pageString = HtmlGenHelper.PAGE_OPEN + replyed
				// + getReplyString(sender, readTopic)
						+ HtmlGenHelper.PAGE_CLOSE;
				handler.sendEmptyMessage(0);
			}
		}.start();

	}

	private void setListeners() {

		headerView.setButtonOnclickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				Bundle bundle = new Bundle();
				intent.setClass(PmViewActivity.this, EditActivity.class);
				bundle.putInt(EditActivity.MOD, EditActivity.MOD_PM);
				bundle.putString(EditActivity.TO_USER, sender);
				StringBuilder tmp = new StringBuilder();
				tmp.append("[quote][b]以下是引用").append(sender).append("在[i]")
						.append(sendTime).append("[/i]时发送的短信：[/b]\n")
						.append(pmContent.replaceAll("(<BR>|<br>)", "\n")).append("[/quote]");
				bundle.putString(EditActivity.PM_CONTENT, tmp.toString());
				bundle.putString(EditActivity.PM_TITLE, readTopic);
				intent.putExtra(EditActivity.BUNDLE, bundle);
				startActivity(intent);
			}
		});
	}

	public void preview(String content) {
		Log.d( TAG, "preview clicked");
		Intent intent = new Intent(this, PreviewActivity.class);
		intent.putExtra("content", content);
		startActivity(intent);
	}

	public void moreEmot() {
		FaceExpressionChooseListener faceListener = new FaceExpressionChooseListener() {

			@Override
			public void onOkClick() {

			}

			@Override
			public void onFaceExpressionClick(String faceExpression) {

				faceChoosedString = faceExpression;
				handler.sendEmptyMessage(1);
			}
		};
		MoreEmotChooseDialog dialog = new MoreEmotChooseDialog(
				PmViewActivity.this, faceListener);
		dialog.show();
	}

	public void open(String pageLink, int pageNum) {
		Log.d("MyCC98", "open new post:" + pageNum);
		Bundle bundle = new Bundle();
		bundle.putString(PostContentsJSActivity.POST_LINK, pageLink);
		bundle.putInt(PostContentsJSActivity.PAGE_NUMBER, pageNum);
		bundle.putString(PostContentsJSActivity.POST_NAME, "");
		Intent intent = new Intent(this, PostContentsJSActivity.class);
		intent.putExtra(PostContentsJSActivity.POST, bundle);
		this.startActivity(intent);
	}
	
}
