package com.adofaimacro.app;

import android.os.IBinder;
import android.os.Parcel;
import android.view.InputEvent;
import android.view.MotionEvent;

import java.lang.reflect.Method;

public class ShizukuHelper {
	
	private static Object inputManager = null;
	
	public static boolean init() {
		try {
			// 1. 拿到 Shizuku 服务的 Binder
			Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
			Method getServiceMethod = serviceManagerClass.getMethod("getService", String.class);
			IBinder shizukuBinder = (IBinder) getServiceMethod.invoke(null, "shizuku");
			if (shizukuBinder == null) return false;
			
			// 2. 通过 Binder 调用 Shizuku 的 getSystemService("input")
			Parcel data = Parcel.obtain();
			Parcel reply = Parcel.obtain();
			data.writeInterfaceToken("moe.shizuku.manager.IShizukuService");
			data.writeString("input");
			shizukuBinder.transact(1, data, reply, 0); // 1 = getSystemService
			IBinder inputBinder = reply.readStrongBinder();
			data.recycle();
			reply.recycle();
			
			if (inputBinder == null) return false;
			
			// 3. 包装成 IInputManager
			Class<?> stubClass = Class.forName("android.hardware.input.IInputManager$Stub");
			Method asInterfaceMethod = stubClass.getMethod("asInterface", IBinder.class);
			inputManager = asInterfaceMethod.invoke(null, inputBinder);
			return inputManager != null;
			} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean injectTouch(MotionEvent event) {
		if (inputManager == null) {
			if (!init()) return false;
		}
		try {
			Method injectMethod = inputManager.getClass().getMethod(
			"injectInputEvent", InputEvent.class, int.class);
			injectMethod.invoke(inputManager, event, 0);
			return true;
			} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}