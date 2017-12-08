package doext.implement;

import java.net.URI;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;
import core.DoServiceContainer;
import core.helper.DoIOHelper;
import core.helper.DoJsonHelper;
import core.interfaces.DoIScriptEngine;
import core.object.DoInvokeResult;
import doext.define.do_WebSocket_IMethod;
import doext.define.do_WebSocket_MAbstract;
import doext.utils.WebSocketUtils;
import doext.websocket.koushikdutta.WebSocketClient;
import doext.websocket.koushikdutta.WebSocketClient.WebSocketListener;

/**
 * 自定义扩展MM组件Model实现，继承do_WebSocket_MAbstract抽象类，并实现do_WebSocket_IMethod接口方法；
 * #如何调用组件自定义事件？可以通过如下方法触发事件：
 * this.model.getEventCenter().fireEvent(_messageName, jsonResult);
 * 参数解释：@_messageName字符串事件名称，@jsonResult传递事件参数对象； 获取DoInvokeResult对象方式new
 * DoInvokeResult(this.getUniqueKey());
 */
public class do_WebSocket_Model extends do_WebSocket_MAbstract implements do_WebSocket_IMethod, WebSocketListener {

	private DoIScriptEngine scriptEngine;
	private String callbackFuncName;
	private WebSocketClient mSocket;

	public do_WebSocket_Model() throws Exception {
		super();
	}

	/**
	 * 同步方法，JS脚本调用该组件对象方法时会被调用，可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V），获取参数值使用API提供DoJsonHelper类；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public boolean invokeSyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		if ("close".equals(_methodName)) {
			this.close(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		return super.invokeSyncMethod(_methodName, _dictParas, _scriptEngine, _invokeResult);
	}

	/**
	 * 异步方法（通常都处理些耗时操作，避免UI线程阻塞），JS脚本调用该组件对象方法时会被调用， 可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V），获取参数值使用API提供DoJsonHelper类；
	 * @_scriptEngine 当前page JS上下文环境
	 * @_callbackFuncName 回调函数名 #如何执行异步方法回调？可以通过如下方法：
	 *                    _scriptEngine.callback(_callbackFuncName
	 *                    ,_invokeResult);
	 *                    参数解释：@_callbackFuncName回调函数名，@_invokeResult传递回调函数参数对象；
	 *                    获取DoInvokeResult对象方式new
	 *                    DoInvokeResult(this.getUniqueKey());
	 */
	@Override
	public boolean invokeAsyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		if ("connect".equals(_methodName)) {
			this.connect(_dictParas, _scriptEngine, _callbackFuncName);
			return true;
		}
		if ("send".equals(_methodName)) {
			this.send(_dictParas, _scriptEngine, _callbackFuncName);
			return true;
		}
		return super.invokeAsyncMethod(_methodName, _dictParas, _scriptEngine, _callbackFuncName);
	}

	/**
	 * 关闭链接；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void close(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		if (mSocket != null) {
			mSocket.disconnect();
			mSocket = null;
		}
	}

	/**
	 * 连接；
	 * 
	 * @throws Exception
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_callbackFuncName 回调函数名
	 */
	@Override
	public void connect(JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		this.scriptEngine = _scriptEngine;
		this.callbackFuncName = _callbackFuncName;
		final String _url = DoJsonHelper.getString(_dictParas, "url", "");
		if (TextUtils.isEmpty(_url)) {
			callBack(false);
			return;
		}
		mSocket = new WebSocketClient(URI.create(_url), do_WebSocket_Model.this, null);
		mSocket.connect();
	}

	/**
	 * 发送数据；
	 * 
	 * @throws Exception
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_callbackFuncName 回调函数名
	 */
	@Override
	public void send(JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		this.scriptEngine = _scriptEngine;
		this.callbackFuncName = _callbackFuncName;
		if (mSocket != null) {
			final String _content = DoJsonHelper.getString(_dictParas, "content", "");
			final String _type = DoJsonHelper.getString(_dictParas, "type", "");
			if (TextUtils.isEmpty(_content)) {
				callBack(false);
				return;
			}
			try {
				if (_type.equalsIgnoreCase("HEX")) {// 发送十六进制数
					mSocket.send(WebSocketUtils.hexStr2Byte(_content));
				} else if (_type.equalsIgnoreCase("File")) {// 发送文件
					String _realPath = DoIOHelper.getLocalFileFullPath(scriptEngine.getCurrentPage().getCurrentApp(), _content);
					mSocket.send(DoIOHelper.readAllBytes(_realPath));
				} else if (_type.equalsIgnoreCase("gbk")) {
					mSocket.send(_content, "GBK");// 发送字符串
				} else {
					mSocket.send(_content);// 发送字符串
				}
				callBack(true);
			} catch (Exception e) {
				callBack(false);
				e.printStackTrace();
				DoServiceContainer.getLogEngine().writeError("发送异常", e);
			}
		} else {
			DoServiceContainer.getLogEngine().writeInfo("发送异常，webSocket没有建立连接", "do_WebSocket");
		}
	}

	public void callBack(boolean result) {
		DoInvokeResult _invokeResult = new DoInvokeResult(getUniqueKey());
		_invokeResult.setResultBoolean(result);
		scriptEngine.callback(callbackFuncName, _invokeResult);
	}

	public void fireReceiveEvent(byte[] data) {
		String msg = WebSocketUtils.bytesToHexString(data, data.length);
		DoInvokeResult _invokeResult = new DoInvokeResult(getUniqueKey());
		_invokeResult.setResultText(msg);
		if (getEventCenter() != null) {
			getEventCenter().fireEvent("receive", _invokeResult);
		}
	}

	public void fireErrorEvent(String msg) throws JSONException {
		DoInvokeResult _invokeResult = new DoInvokeResult(getUniqueKey());
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("msg", msg);
		_invokeResult.setResultNode(jsonObject);
		if (getEventCenter() != null) {
			getEventCenter().fireEvent("error", _invokeResult);
		}
	}

	// ///////////////////////////////////////////////////////////////
	@Override
	public void onConnect() {
		callBack(true);
	}

	@Override
	public void onMessage(String message) {
		fireReceiveEvent(message.getBytes());
	}

	@Override
	public void onMessage(byte[] message) {
		fireReceiveEvent(message);
	}

	@Override
	public void onDisconnect(int code, String reason) {
		callBack(false);
	}

	@Override
	public void onError(Exception error) {
		callBack(false);
		try {
			fireErrorEvent(error.getMessage());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	// ////////////////////////////////////////////////////////////////
}