/*
 * “Commons Clause” License, https://commonsclause.com/
 *
 * The Software is provided to you by the Licensor under the License,
 * as defined below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights
 * under the License will not include, and the License does not grant to
 * you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of
 * the rights granted to you under the License to provide to third parties,
 * for a fee or other consideration (including without limitation fees for
 * hosting or consulting/support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from
 * the functionality of the Software. Any license notice or attribution
 * required by the License must also include this Commons Clause License
 * Condition notice.
 *
 * Software:    NLPCraft
 * License:     Apache 2.0, https://www.apache.org/licenses/LICENSE-2.0
 * Licensor:    Copyright (C) 2018 DataLingvo, Inc. https://www.datalingvo.com
 *
 *     _   ____      ______           ______
 *    / | / / /___  / ____/________ _/ __/ /_
 *   /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/
 *  / /|  / / /_/ / /___/ /  / /_/ / __/ /_
 * /_/ |_/_/ .___/\____/_/   \__,_/_/  \__/
 *        /_/
 */

package org.nlpcraft.examples.misc.apixu.beans;

import com.google.gson.annotations.SerializedName;

/**
 * REST parsing bean.
 */
public class Day {
    @SerializedName("maxtemp_c") private Double maxTempC;
    @SerializedName("maxtemp_f") private Double maxTempF;
    @SerializedName("mintemp_c") private Double minTempC;
    @SerializedName("mintemp_f") private Double minTempF;
    @SerializedName("avgtemp_c") private Double avgTempC;
    @SerializedName("avgtemp_f") private Double avgTempF;
    @SerializedName("maxwind_mph") private Double maxWindMph;
    @SerializedName("maxwind_kph") private Double maxWindKph;
    @SerializedName("totalprecip_mm") private Double totalPrecipMm;
    @SerializedName("totalprecip_in") private Double totalPrecipIn;
    @SerializedName("avgvis_km") private Double avgVisKm;
    @SerializedName("avgvis_miles") private Double avgVisMiles;
    @SerializedName("avghumidity") private int avgHumidity;
    private Condition condition;
    private Double uv;

    public Double getMaxTempC() {
        return maxTempC;
    }

    public void setMaxTempC(Double maxTempC) {
        this.maxTempC = maxTempC;
    }

    public Double getMaxTempF() {
        return maxTempF;
    }

    public void setMaxTempF(Double maxTempF) {
        this.maxTempF = maxTempF;
    }

    public Double getMinTempC() {
        return minTempC;
    }

    public void setMinTempC(Double minTempC) {
        this.minTempC = minTempC;
    }

    public Double getMinTempF() {
        return minTempF;
    }

    public void setMinTempF(Double minTempF) {
        this.minTempF = minTempF;
    }

    public Double getAvgTempC() {
        return avgTempC;
    }

    public void setAvgTempC(Double avgTempC) {
        this.avgTempC = avgTempC;
    }

    public Double getAvgTempF() {
        return avgTempF;
    }

    public void setAvgTempF(Double avgTempF) {
        this.avgTempF = avgTempF;
    }

    public Double getMaxWindMph() {
        return maxWindMph;
    }

    public void setMaxWindMph(Double maxWindMph) {
        this.maxWindMph = maxWindMph;
    }

    public Double getMaxWindKph() {
        return maxWindKph;
    }

    public void setMaxWindKph(Double maxWindKph) {
        this.maxWindKph = maxWindKph;
    }

    public Double getTotalPrecipMm() {
        return totalPrecipMm;
    }

    public void setTotalPrecipMm(Double totalPrecipMm) {
        this.totalPrecipMm = totalPrecipMm;
    }

    public Double getTotalPrecipIn() {
        return totalPrecipIn;
    }

    public void setTotalPrecipIn(Double totalPrecipIn) {
        this.totalPrecipIn = totalPrecipIn;
    }

    public Double getAvgVisKm() {
        return avgVisKm;
    }

    public void setAvgVisKm(Double avgVisKm) {
        this.avgVisKm = avgVisKm;
    }

    public Double getAvgVisMiles() {
        return avgVisMiles;
    }

    public void setAvgVisMiles(Double avgVisMiles) {
        this.avgVisMiles = avgVisMiles;
    }

    public int getAvgHumidity() {
        return avgHumidity;
    }

    public void setAvgHumidity(int avgHumidity) {
        this.avgHumidity = avgHumidity;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    public Double getUv() {
        return uv;
    }

    public void setUv(Double uv) {
        this.uv = uv;
    }
}
