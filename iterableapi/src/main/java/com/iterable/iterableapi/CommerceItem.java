package com.iterable.iterableapi;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Represents a product. These are used by the commerce API; see {@link IterableApi#trackPurchase(double, List, JSONObject)}
 */
public class CommerceItem {

    /** id of this product */
    public final String id;

    /** name of this product */
    public final String name;

    /** price of this product */
    public final double price;

    /** quantity of this product */
    public final int quantity;

    /**
     * Creates a {@link CommerceItem} with the specified properties
     * @param id         id of the product
     * @param name       name of the product
     * @param price      price of the product
     * @param quantity   quantity of the product
     */
    public CommerceItem(String id, String name, double price, int quantity) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }

    /**
     * A JSONObject representation of this item
     * @return A JSONObject representing this item
     * @throws JSONException
     */
    public JSONObject toJSONObject() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("name", name);
        jsonObject.put("price", price);
        jsonObject.put("quantity", quantity);
        return jsonObject;
    }
}
