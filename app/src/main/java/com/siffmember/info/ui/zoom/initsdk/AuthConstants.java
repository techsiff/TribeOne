package com.siffmember.info.ui.zoom.initsdk;

public interface AuthConstants {

	// TODO Change it to your web domain
	public final static String WEB_DOMAIN = "zoom.us";

	/**
	 * We recommend that, you can generate jwttoken on your own server instead of hardcore in the code.
	 * We hardcore it here, just to run the demo.
	 *
	 * You can generate a jwttoken on the https://jwt.io/
	 * with this payload:
	 * {
	 *
	 *     "appKey": "string", // app key
	 *     "iat": long, // access token issue timestamp
	 *     "exp": long, // access token expire time
	 *     "tokenExp": long // token expire time
	 * }
	 */
    public final static String SDK_JWTTOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhcHBLZXkiOiJMdFlIQ25OV1M2R05CQ2VRRVNGX2N3Iiwic2RrS2V5IjoiTHRZSENuTldTNkdOQkNlUUVTRl9jdyIsIm1uIjoiODc2MTczMDE0ODgiLCJyb2xlIjowLCJ0b2tlbkV4cCI6MTc2Njc2NjI2NiwiaWF0IjoxNzY2NzYyNjY2LCJleHAiOjE3NjY3NjYyNjZ9.VmbLKg2kD44cTiM9IglT_pTIjcqSXC9uzVqI5Ukx8Ps";

}
