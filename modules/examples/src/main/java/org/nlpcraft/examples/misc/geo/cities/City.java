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
 * Software:    NlpCraft
 * License:     Apache 2.0, https://www.apache.org/licenses/LICENSE-2.0
 * Licensor:    DataLingvo, Inc. https://www.datalingvo.com
 *
 *     _   ____      ______           ______
 *    / | / / /___  / ____/________ _/ __/ /_
 *   /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/
 *  / /|  / / /_/ / /___/ /  / /_/ / __/ /_
 * /_/ |_/_/ .___/\____/_/   \__,_/_/  \__/
 *        /_/
 */

package org.nlpcraft.examples.misc.geo.cities;

/**
 * City data object.
 */
public class City {
    private String name;
    private String country;

    /**
     * Creates new city object.
     *
     * @param name City name.
     * @param country City country.
     */
    public City(String name, String country) {
        this.name = name;
        this.country = country;
    }

    /**
     * Gets city name.
     *
     * @return City name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets city country.
     *
     * @return City country.
     */
    public String getCountry() {
        return country;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        City city = (City) o;

        return (name != null ? name.equals(city.name) : city.name == null) &&
            (country != null ? country.equals(city.country) : city.country == null);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;

        result = 31 * result + (country != null ? country.hashCode() : 0);

        return result;
    }
}
