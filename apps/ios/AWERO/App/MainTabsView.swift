import SwiftUI

struct AweroMainTabsView: View {
    @Binding var showingCreate: Bool
    @State private var selectedTab = 0

    var body: some View {
        TabView(selection: $selectedTab) {
            HomeView(showingCreate: $showingCreate)
                .tabItem { Label("nav.home", systemImage: "house.fill") }
                .tag(0)

            AweroProgressView(onSetAlarm: {
                selectedTab = 0
                showingCreate = true
            })
            .tabItem { Label("nav.progress", systemImage: "chart.bar.fill") }
            .tag(1)

            NavigationStack {
                SettingsView()
            }
            .tabItem { Label("nav.profile", systemImage: "person.fill") }
            .tag(2)
        }
        .tint(AweroDesign.coral)
    }
}
