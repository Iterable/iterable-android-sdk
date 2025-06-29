package com.iterable.iterableapi

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Represents a product. These are used by the commerce API; see [IterableApi.trackPurchase]
 */
class CommerceItem {

    /** id of this product */
    val id: String

    /** name of this product */
    val name: String

    /** price of this product */
    val price: Double

    /** quantity of this product */
    val quantity: Int

    /** SKU of this product **/
    val sku: String?

    /** description of this product **/
    val description: String?

    /** URL of this product **/
    val url: String?

    /** URL of this product's image **/
    val imageUrl: String?

    /** categories of this product, in breadcrumb list form **/
    val categories: Array<String>?

    /** data fields as part of this product **/
    val dataFields: JSONObject?

    /**
     * Creates a [CommerceItem] with the specified properties
     * @param id         id of the product
     * @param name       name of the product
     * @param price      price of the product
     * @param quantity   quantity of the product
     */
    constructor(
        id: String,
        name: String,
        price: Double,
        quantity: Int
    ) : this(id, name, price, quantity, null, null, null, null, null, null)

    /**
     * Creates a [CommerceItem] with the specified properties
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
    constructor(
        id: String,
        name: String,
        price: Double,
        quantity: Int,
        sku: String?,
        description: String?,
        url: String?,
        imageUrl: String?,
        categories: Array<String>?,
        dataFields: JSONObject?
    ) {
        this.id = id
        this.name = name
        this.price = price
        this.quantity = quantity
        this.sku = sku
        this.description = description
        this.url = url
        this.imageUrl = imageUrl
        this.categories = categories
        this.dataFields = dataFields
    }

    /**
     * A JSONObject representation of this item
     * @return A JSONObject representing this item
     * @throws JSONException
     */
    @NonNull
    @Throws(JSONException::class)
    fun toJSONObject(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("id", id)
        jsonObject.put("name", name)
        jsonObject.put("price", price)
        jsonObject.put("quantity", quantity)
        jsonObject.putOpt("sku", sku)
        jsonObject.putOpt("description", description)
        jsonObject.putOpt("url", url)
        jsonObject.putOpt("imageUrl", imageUrl)
        jsonObject.putOpt("dataFields", dataFields)

        categories?.let { cats ->
            val categoriesArray = JSONArray()
            for (category in cats) {
                categoriesArray.put(category)
            }
            jsonObject.put("categories", categoriesArray)
        }

        return jsonObject
    }
}
