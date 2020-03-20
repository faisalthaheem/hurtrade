[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/hurtrade?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

[![Build Status](https://travis-ci.org/faisalthaheem/hurtrade.svg?branch=master)](https://travis-ci.org/faisalthaheem/hurtrade.svg?branch=master) [![Issues](https://img.shields.io/github/issues/faisalthaheem/hurtrade.svg?style=flat-square)](https://github.com/faisalthaheem/hurtrade/issues)


# HUR (حر) TRADE
**Hur** (Free) **Trade** is a an open source trading platform that can be used to trade currency pairs, valuables etc. While this platform provides the means to track and process transactions, it does not provide anything related to the policies, compliance or processes needed to run a trading business.

*See [Wiki](https://github.com/faisalthaheem/hurtrade/wiki/Running-HurTrade) for instructions on how to quickly setup and test the platform.*

Following video shows a quick side by side overview of logging in, placing orders and updating statuses from the point of view of a trader and a back office dealer.

[![overview thumbnail](https://j.gifs.com/1jxYER.gif)](https://www.youtube.com/watch?v=TXtN0MgiWFs&feature=youtu.be)

And another one showing the services starting up using docker-compose
[![overview thumbnail](https://j.gifs.com/NxQEjL.gif)](https://youtu.be/2Xlu4F-Slkw)

# About the project
This project is part of 3, which are

Project|Link|Description
-------|----|-----------
Hur Trade|This project|The backend system which processes everything.
Hur Trade Clients|https://github.com/faisalthaheem/hurtrade-clients|The front end for use by end-users, contains the projects for traders and dealers.
Hur Trade Docker|https://github.com/faisalthaheem/hurtrade-docker|Docker files for creating images from published releases of Hur Trade. These are also published on docker hub and provide the quickest way to test out the software.

# Current State
The project currently supports following features and is still in its infant stage, any contribution is highly appreciated.

 - Order Management (new orders, cancelling pending, closing opened etc)
 - Order Re-quoting
 - Scheduled Trading (by definition of a schedule specifying hours between which trading is allowed)
 - Data Persistence on shutdown and startup
 - Distinctively Restricting Trades by Quantity to users
 - Distinctively Charging Fee and Commissions to users
 - Margin Calls
 - Account liquidification flag 
 - Floating Statuses for Back office
 - Cover Accounts and Net Trade Position for Back office

# Reporting Issues
Please report issues on their respective repositories.



# Other notes
USD as the only account currency is supported at the moment.
