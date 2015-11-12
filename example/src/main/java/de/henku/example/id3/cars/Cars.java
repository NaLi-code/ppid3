/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Hendrik Kunert
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.henku.example.id3.cars;

import com.opencsv.CSVReader;
import de.henku.algorithm.id3_horizontal.Attribute;
import de.henku.algorithm.id3_horizontal.DataLayer;
import de.henku.algorithm.id3_horizontal.SecureID3;
import de.henku.algorithm.id3_horizontal.SquareDivisionLastController;
import de.henku.algorithm.id3_horizontal.communication.NodeValuePair;
import de.henku.algorithm.id3_horizontal.tree.ID3Node;
import de.henku.example.id3.utils.ListAttributeBuilder;
import de.henku.example.id3.utils.ListDataLayer;
import de.henku.jpaillier.KeyPair;
import de.henku.jpaillier.KeyPairBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Create ID3 classification tree for "Car Evaluation Data Set" from
 * UCI - Machine learning repository.
 * <br><br>
 * Dataset:
 * <ul>
 *  <li>https://archive.ics.uci.edu/ml/datasets/Car+Evaluation</li>
 *  <li>Lichman, M. (2013). UCI Machine Learning Repository [http://archive.ics.uci.edu/ml]. Irvine, CA: University of California, School of Information and Computer Science.</li>
 * </ul>
 */
public class Cars {

    public static final String DATASET_URL = "de/henku/example/id3/cars.data";

    public static void main(String[] args) throws IOException {
        List<CarsListRow> transactions = loadData();
        Collections.shuffle(transactions);

        // load attributes
        List<Attribute> attributes = extractAttributes(transactions);

        // load class attribute
        Attribute playBall = new ListAttributeBuilder<CarsListRow>("classValue").from_transactions(transactions);

        int half = transactions.size() / 2;
        List<CarsListRow> transactions1 = transactions.subList(0, half);
        List<CarsListRow> transactions2 = transactions.subList(half, transactions.size());

        KeyPair keyPair = new KeyPairBuilder().bits(128)
                .generateKeyPair();

        DataLayer dataLayerSlave = new ListDataLayer<>(transactions2, playBall);
        SquareDivisionLastController slave =
                new SquareDivisionLastController(dataLayerSlave, keyPair.getPublicKey());

        DataLayer dataLayerMaster = new ListDataLayer<>(transactions1, playBall);

        SecureID3 id3 = new SecureID3(dataLayerMaster, slave, keyPair);
        slave.setReceiver(id3.getController());

        List<NodeValuePair> path = new ArrayList<>();
        ID3Node tree = id3.run(attributes, path);

        // show resulting tree
        System.out.println(tree);
    }

    private static List<Attribute> extractAttributes(List<CarsListRow> transactions) {
        Attribute buying = new ListAttributeBuilder<CarsListRow>("buying").from_transactions(transactions);
        Attribute maint = new ListAttributeBuilder<CarsListRow>("maint").from_transactions(transactions);
        Attribute doors = new ListAttributeBuilder<CarsListRow>("doors").from_transactions(transactions);
        Attribute persons = new ListAttributeBuilder<CarsListRow>("persons").from_transactions(transactions);
        Attribute lug_boot = new ListAttributeBuilder<CarsListRow>("lug_boot").from_transactions(transactions);
        Attribute safety = new ListAttributeBuilder<CarsListRow>("safety").from_transactions(transactions);

        return Arrays.asList(
                buying,
                maint,
                doors,
                persons,
                lug_boot,
                safety
        );
    }

    private static List<CarsListRow> loadData() throws IOException {
        List<CarsListRow> transactions = new ArrayList<>();

        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        URL url = classloader.getResource(DATASET_URL);
        assert url != null;
        File file = new File(url.getFile());
        CSVReader reader = new CSVReader(new FileReader(file));

        String[] nextLine;
        while ((nextLine = reader.readNext()) != null) {
            if (nextLine.length != 7) {
                throw new IOException("not enough rows");
            }

            transactions.add(new CarsListRow(
                    nextLine[0],
                    nextLine[1],
                    nextLine[2],
                    nextLine[3],
                    nextLine[4],
                    nextLine[5],
                    nextLine[6]
            ));
        }

        return transactions;
    }

}
