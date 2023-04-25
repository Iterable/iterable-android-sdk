package com.iterable.iterableapi;

/**
 * Handler to pass result of success/failure for setemail/setuserid
 */
public interface ResultCallbackHandler {

 /**
  * Callback called when registertoken API hits
  * @param success can be true or false based on result of registertoken API success/failure
  */
   void sendResult(boolean success);
}
