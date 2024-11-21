package com.iterable.iterableapi.ddl;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Criteria {
    public final String criteriaId;
    public final List<CriteriaList> criteriaList;

    public Criteria(String criteriaId, List<CriteriaList> criteriaList) {
        this.criteriaId = criteriaId;
        this.criteriaList = criteriaList;
    }

    public static Criteria fromJSONObject(JSONObject json) throws JSONException {

        List<CriteriaList> criteriaList = new ArrayList<>();
        JSONArray jsonArray = json.getJSONArray("criteriaList");
        for (int i = 0; i < jsonArray.length(); i++) {
            criteriaList.add(CriteriaList.fromJSONObject(jsonArray.getJSONObject(i)));
        }
        return new Criteria(
                json.getString("criteriaId"),
                criteriaList
        );
    }

    public static class CriteriaList {
        public final String criteriaType;
        public final String comparator;
        public final String name;
        public final int aggregateCount;

        public CriteriaList(String criteriaType, String comparator, String name, int aggregateCount) {
            this.criteriaType = criteriaType;
            this.comparator = comparator;
            this.name = name;
            this.aggregateCount = aggregateCount;
        }

        static CriteriaList fromJSONObject(JSONObject json) throws JSONException {
            return new CriteriaList(
                    json.getString("criteriaType"),
                    json.getString("comparator"),
                    json.getString("name"),
                    json.getInt("aggregateCount")
            );
        }
    }

}