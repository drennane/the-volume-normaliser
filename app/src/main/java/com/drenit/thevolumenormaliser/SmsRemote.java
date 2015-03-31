/*
 * Copyright (C) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.drenit.thevolumenormaliser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telephony.gsm.SmsMessage;
import android.util.Log;

/**
 * 
 * Register for receiving SMS messages. Scan all incoming SMS for the presence of the
 * activation code followed by one of the command words. If this pattern is found,
 * the the SmsRemoteReceiver is called.
 */
public class SmsRemote  extends BroadcastReceiver {
	public interface SmsRemoteReceiver {
		void receive(String cmd);
	}
	Boolean mActive = false;
	Context mContext;
	String mCode; 
	String[] mCommands;
	
	/**
	 * BroadcastReceiver framework method.
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i("SMS", "received SMS message");
		Bundle bundle = intent.getExtras();
		if (bundle == null) return; 

		Object pdus[] = (Object[]) bundle.get("pdus");
		for (int n = 0; n < pdus.length; n++) {
			SmsMessage message = SmsMessage.createFromPdu((byte[]) pdus[n]);

			String msg = message.getDisplayMessageBody();
			Log.i("SMS", msg);
			msg = msg.toLowerCase();
			if (msg.indexOf(mCode) != -1) {
				for (int i=0; i< mCommands.length; i++) {
					if (msg.indexOf(mCode+" "+mCommands[i]) != -1) {
						((SmsRemoteReceiver)mContext).receive(mCommands[i]);
						return;
					}
				}
			}
		}
	}
	/**
	 * Activate SMS remote control receiver.
	 * 
	 * @param context Context object which implements SmsRemoteReceiver interface.
	 * @param code Code string included in valid remote control SMS messages.
	 * @param commands Command string following the authorization code.
	 */
	public void register(Context context, String code, String[] commands) {
		mContext = context;
		mCode = code;
		mCommands = commands;
		if(mActive) return;
		IntentFilter smsFilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
		context.registerReceiver(this, smsFilter);
		mActive = true;
		Log.i("SMS", "register");
	};
	
	/**
	 * Deactivate SMS registration
	 */
	public void deregister() {
		if (!mActive) return;
		mContext.unregisterReceiver(this);
		mActive = false;
		Log.i("SMS", "deregister");
	}
}
