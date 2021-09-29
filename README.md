# Table2Model

[![License (MIT)](https://img.shields.io/badge/license-MIT-blue.svg?style=plastic)](https://opensource.org/licenses/MIT)
[![Latest version](https://img.shields.io/badge/Latest_version-1.0-brightgreen.svg?style=plastic)](https://github.com/draeger-lab/Table2Model/releases/)
![Code Size](https://img.shields.io/github/languages/code-size/draeger-lab/Table2Model.svg?style=plastic)

A parser for systems biology models published in a table form that produces standardized files, such as SBML.

This project provides useful Java™ classes to read systems biology models from character-separated table files and writes them to standardized community formats, such as [SBML](http://sbml.org) using the [JSBML](https://github.com/sbmlteam/JSBML) library.

One example use case to try out this package would be the the model <i>i</i>CW773 that was given in the form of two tables in the [publication](https://identifiers.org/pubmed/28680478).
Initially, the code in this package has been tailored to the format of this specific model, but could be useful in other circumstances, too.

To build an executable from this project run the following:
```
mvn assembly:assembly package
```
It will automatically create the file `table2model-1.0-jar-with-dependencies.jar` within the `target/` directory of this project.

Run the executable as follows:
```
java -jar target/table2model-1.0-jar-with-dependencies.jar /path/to/metabolites.csv /path/to/reactions.csv /path/to/output.xml
```
