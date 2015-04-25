package org.kotemaru.android.bizcard.model;

import org.kotemaru.android.bizcard.R;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

public enum Kind {
	// @formatter:off
	NIL(0),
	ACCESS_DATE		(R.string.kind_access_date	),
	CREATE_DATE		(R.string.kind_create_date	),
	UPDATE_DATE		(R.string.kind_update_date	),
	COMPANY			(R.string.kind_company		,R.id.kind_company),
	ORGANIZATION	(R.string.kind_organization	,R.id.kind_organization	),
	POSITION		(R.string.kind_position		,R.id.kind_position		),
	NAME			(R.string.kind_name			,R.id.kind_name			),
	KANA			(R.string.kind_kana			,R.id.kind_kana			),
	ADDRESS			(R.string.kind_address		,R.id.kind_address		),
	TEL				(R.string.kind_tel			,R.id.kind_tel			),
	MOBILE			(R.string.kind_mobile		,R.id.kind_mobile		),
	FAX				(R.string.kind_fax			,R.id.kind_fax			),
	EMAIL			(R.string.kind_email		,R.id.kind_email		),
	WEB				(R.string.kind_web			,R.id.kind_web			),
	NONTS			(R.string.kind_notes		,R.id.kind_notes		),
	IMAGE_URL		(R.string.kind_image_url	),
	;
	// @formatter:on
	private static final int ZERO = 0;

	public final int labelResId;
	public final int viewResId;

	Kind(int labelResId) {
		this(labelResId, 0);
	}
	Kind(int labelResId, int textViewResId) {
		this.labelResId = labelResId;
		this.viewResId = textViewResId;
	}

	public static Kind toKind(String name) {
		if (name == null) return Kind.NIL;
		return Kind.valueOf(name);
	}

	public String getLabel(Context context) {
		if (labelResId == ZERO) return null;
		return context.getString(labelResId);
	}
	public CharSequence getValue(CardModel model) {
		if (model == null) return null;
		return model.get(this);
	}
	public void setValue(CardModel model, CharSequence text) {
		if (model == null) return;
		model.put(this, text);
	}
	public View getView(View parent) {
		if (viewResId == ZERO) return null;
		return parent.findViewById(viewResId);
	}
	public TextView getTextView(View parent) {
		return (TextView) getView(parent);
	}
	public String getText(View parent) {
		TextView textView = getTextView(parent);
		if (textView == null) return null;
		return textView.getText().toString();
	}
	public void setText(View parent, String text) {
		TextView textView = getTextView(parent);
		if (textView == null) return;
		textView.setText(text);
	}

	public void viewToModel(View parent, CardModel model) {
		TextView textView = getTextView(parent);
		if (textView == null) return;
		model.put(this, textView.getText());
	}
	public void modelToView(CardModel model, View parent) {
		TextView textView = getTextView(parent);
		if (textView == null) return;
		if (model == null) {
			textView.setText(null);
		} else {
			textView.setText(model.get(this));
		}
	}
}
