/*
 * Copyright 2016 Faisal Thaheem <faisal.ajmal@gmail.com>.
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
