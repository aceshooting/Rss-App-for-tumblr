/**
 * Ace Shooting
 *
 * Copyright (c) 2014 Ace Shooting
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package com.aceshooting.rssapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.aceshooting.rssapp.service.FetcherService;

public class RefreshBroadcastReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		if (Strings.ACTION_REFRESHFEEDS.equals(intent.getAction())) {
			context.startService(new Intent(context, FetcherService.class).putExtras(intent)); // a thread would mark the process as inactive
		} else if (Strings.ACTION_STOPREFRESHFEEDS.equals(intent.getAction())) {
			context.stopService(new Intent(context, FetcherService.class));
		}
	}
	
}
