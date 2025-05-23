package com.example.trackmate1.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.example.trackmate1.ui.theme.Trackmate1Theme


@Composable
fun SearchScreen(){
    Box(modifier = Modifier.fillMaxSize()){
        Column(modifier = Modifier.fillMaxSize().align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(text = "search Screen", fontSize = 20.sp, color = Color.Black)

        }
    }
}

@Preview
@Composable
fun SearchPreview(){
    Trackmate1Theme  {
        SearchScreen()
    }
}