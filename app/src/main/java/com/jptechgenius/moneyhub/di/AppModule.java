package com.jptechgenius.moneyhub.di;

import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {
    // Database and DAO providers have been moved to DatabaseModule.java
    // Repositories are provided in RepositoryModule.java
}