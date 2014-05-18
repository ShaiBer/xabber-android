/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 * 
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 * 
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.AccountType;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.ui.adapter.AccountTypeAdapter;
import com.xabber.android.ui.helper.ManagedActivity;
import com.xabber.android.aniwaysdemo.R;

public class AccountAdd extends ManagedActivity implements
		View.OnClickListener, OnItemSelectedListener {

	private static final String SAVED_ACCOUNT_TYPE = "com.xabber.android.ui.AccountAdd.ACCOUNT_TYPE";

	private static final int OAUTH_WML_REQUEST_CODE = 1;

	private CheckBox storePasswordView;
	private CheckBox syncableView;
	private Spinner accountTypeView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (isFinishing())
			return;

		setContentView(R.layout.account_add);

		storePasswordView = (CheckBox) findViewById(R.id.store_password);
		syncableView = (CheckBox) findViewById(R.id.syncable);
		if (!Application.getInstance().isContactsSupported()) {
			syncableView.setVisibility(View.GONE);
			syncableView.setChecked(false);
		}

		accountTypeView = (Spinner) findViewById(R.id.account_type);
		accountTypeView.setAdapter(new AccountTypeAdapter(this));
		accountTypeView.setOnItemSelectedListener(this);

		String accountType;
		if (savedInstanceState == null)
			accountType = null;
		else
			accountType = savedInstanceState.getString(SAVED_ACCOUNT_TYPE);
		accountTypeView.setSelection(0);
		for (int position = 0; position < accountTypeView.getCount(); position++)
			if (((AccountType) accountTypeView.getItemAtPosition(position))
					.getName().equals(accountType)) {
				accountTypeView.setSelection(position);
				break;
			}

		((Button) findViewById(R.id.ok)).setOnClickListener(this);
		InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(findViewById(R.id.ok)
				.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(SAVED_ACCOUNT_TYPE,
				((AccountType) accountTypeView.getSelectedItem()).getName());
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == OAUTH_WML_REQUEST_CODE) {
			if (resultCode == RESULT_OK && !OAuthActivity.isInvalidated(data)) {
				String token = OAuthActivity.getToken(data);
				if (token == null) {
					Application.getInstance().onError(
							R.string.AUTHENTICATION_FAILED);
				} else {
					String account;
					try {
						account = AccountManager.getInstance()
								.addAccount(
										null,
										token,
										(AccountType) accountTypeView
												.getSelectedItem(),
										syncableView.isChecked(),
										storePasswordView.isChecked());
					} catch (NetworkException e) {
						Application.getInstance().onError(e);
						return;
					}
					setResult(RESULT_OK,
							createAuthenticatorResult(this, account));
					finish();
				}
			}
		}
	}

	@Override
	public void onClick(View view) {
		int id = view.getId();
		if (id == R.id.ok) {
			AccountType accountType = (AccountType) accountTypeView
					.getSelectedItem();
			if (accountType.getProtocol().isOAuth()) {
				startActivityForResult(
						OAuthActivity.createIntent(this,
								accountType.getProtocol()),
						OAUTH_WML_REQUEST_CODE);
			} else {
				EditText userView = (EditText) findViewById(R.id.account_user_name);
				EditText passwordView = (EditText) findViewById(R.id.account_password);
				String account;
				try {
					account = AccountManager.getInstance().addAccount(
							userView.getText().toString(),
							passwordView.getText().toString(), accountType,
							syncableView.isChecked(),
							storePasswordView.isChecked());
				} catch (NetworkException e) {
					Application.getInstance().onError(e);
					return;
				}
				setResult(RESULT_OK, createAuthenticatorResult(this, account));
				finish();
			}
		} else {
		}
	}

	@Override
	public void onItemSelected(AdapterView<?> adapterView, View view,
			int position, long id) {
		AccountType accountType = (AccountType) accountTypeView
				.getSelectedItem();
		if (accountType.getProtocol().isOAuth())
			findViewById(R.id.auth_panel).setVisibility(View.GONE);
		else
			findViewById(R.id.auth_panel).setVisibility(View.VISIBLE);
		((TextView) findViewById(R.id.account_user_name)).setHint(accountType
				.getHint());
		((TextView) findViewById(R.id.account_help)).setText(accountType
				.getHelp());
	}

	@Override
	public void onNothingSelected(AdapterView<?> adapterView) {
		accountTypeView.setSelection(0);
	}

	public static Intent createIntent(Context context) {
		return new Intent(context, AccountAdd.class);
	}

	private static Intent createAuthenticatorResult(Context context,
			String account) {
		return new AccountIntentBuilder(null, null).setAccount(account).build();
	}

	public static String getAuthenticatorResultAccount(Intent intent) {
		return AccountIntentBuilder.getAccount(intent);
	}

}
