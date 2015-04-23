package org.kotemaru.android.bizcard.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.SurfaceView;

public class OverlaySurfaceView extends SurfaceView {

	public interface OverlaySurfaceListener {
		public void onDraw(Canvas canvas);
	}

	private OverlaySurfaceListener mListener;

	public OverlaySurfaceView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setWillNotDraw(false);
		// setLayerType(View.LAYER_TYPE_SOFTWARE, null);
	}

	public OverlaySurfaceView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public OverlaySurfaceView(Context context) {
		this(context, null, 0);
	}

	public OverlaySurfaceListener getOverlaySurfaceListener() {
		return mListener;
	}

	public void setOverlaySurfaceListener(OverlaySurfaceListener listener) {
		mListener = listener;
	}

	@SuppressLint("WrongCall")
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (mListener != null) mListener.onDraw(canvas);
	}

}
