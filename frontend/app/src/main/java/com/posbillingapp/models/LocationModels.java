package com.posbillingapp.models;

import com.google.gson.annotations.SerializedName;

public class LocationModels {
    
    public static class Country {
        @SerializedName("id")
        public int id;
        @SerializedName("countryName")
        public String countryName;

        @Override
        public String toString() {
            return countryName;
        }
    }

    public static class State {
        @SerializedName("id")
        public int id;
        @SerializedName("stateName")
        public String stateName;

        @Override
        public String toString() {
            return stateName;
        }
    }

    public static class City {
        @SerializedName("id")
        public int id;
        @SerializedName("cityName")
        public String cityName;

        @Override
        public String toString() {
            return cityName;
        }
    }
}
