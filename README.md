# Table2Model
A parser for systems biology models published in a table form that produces standardized files, such as SBML.

![Code Size](https://img.shields.io/github/languages/code-size/draeger-lab/Table2Model.svg?style=plastic)

This project provides useful Java™ classes to read systems biology models from character-separated table files and writes them to standardized community formats, such as [SBML](http://sbml.org) using the [JSBML](https://github.com/sbmlteam/JSBML) library.

One example use case to try out this package would be the the model <i>i</i>CW773 that was given in the form of two tables in the [publication](https://identifiers.org/pubmed/28680478).
Initially, the code in this package has been taylored to the format of this specific model, but could be useful in other circumstances, too.
