package com.iterable.iterableapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
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

    /** SKU of this product **/
    @Nullable
    public final String sku;

    /** description of this product **/
    @Nullable
    public final String description;

    /** URL of this product **/
    @Nullable
    public final String url;

    /** URL of this product's image **/
    @Nullable
    public final String imageUrl;

    /** categories of this product, in breadcrumb list form **/
    @Nullable
    public final String[] categories;

    /** data fields as part of this product **/
    @Nullable
    public final JSONObject dataFields;

    /**
     * Creates a {@link CommerceItem} with the specified properties
     * @param id         id of the product
     * @param name       name of the product
     * @param price      price of the product
     * @param quantity   quantity of the product
     */
    public CommerceItem(@NonNull String id,
                        @NonNull String name,
                        double price,
                        int quantity) {
        this(id, name, price, quantity, null, null, null, null, null, null);
    }

    /**
     * Creates a {@link CommerceItem} with the specified properties
     * @param id            id of the product
     * @param name          name of the product
     * @param price         price of the product
     * @param quantity      quantity of the product
     * @param sku           SKU of the product
     * @param description   description of the product
     * @param url           URL of the product
     * @param imageUrl      URL of the product's image
     * @param categories    categories this product belongs to
     * @param dataFields    data fields for this CommerceItem
     */
    public CommerceItem(@NonNull String id,
                        @NonNull String name,
                        double price,
                        int quantity,
                        @Nullable String sku,
                        @Nullable String description,
                        @Nullable String url,
                        @Nullable String imageUrl,
                        @Nullable String[] categories,
                        @Nullable JSONObject dataFields) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.sku = sku;
        this.description = description;
        this.url = url;
        this.imageUrl = imageUrl;
        this.categories = categories;
        this.dataFields = dataFields;
    }

    /**
     * A JSONObject representation of this item
     * @return A JSONObject representing this item
     * @throws JSONException
     */
    @NonNull
    public JSONObject toJSONObject() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("name", name);
        jsonObject.put("price", price);
        jsonObject.put("quantity", quantity);
        jsonObject.putOpt("sku", sku);
        jsonObject.putOpt("description", description);
        jsonObject.putOpt("url", url);
        jsonObject.putOpt("imageUrl", imageUrl);
        jsonObject.putOpt("dataFields", dataFields);

        if (categories != null) {
            JSONArray categoriesArray = new JSONArray();
            for (String category : categories) {
                categoriesArray.put(category);
            }
            jsonObject.put("categories", categoriesArray);
        }

        return jsonObject;
    }
}
