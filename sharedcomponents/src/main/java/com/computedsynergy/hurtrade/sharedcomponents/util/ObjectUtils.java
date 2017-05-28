package com.computedsynergy.hurtrade.sharedcomponents.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

/**
 * Created by faisal.t on 5/25/2017.
 */
public class ObjectUtils {



    public static String ObjectToJson(Object o){

        Gson gson = new Gson();

        return gson.toJson(o);
    }

    public static <T> T convertJsonToPOJO(String json, Class<T> target) throws Exception {

        Gson gson = new Gson();

        return target.cast(gson.fromJson(json, target));

    }

}
