import SwiftUI

struct AweroMainTabsView: View {
    @Binding var showingCreate: Bool
    @State private var selectedTab = 0

    var body: some View {
        VStack(spacing: 0) {
            Group {
                switch selectedTab {
                case 1:
                    AweroProgressView(onSetAlarm: {
                        selectedTab = 0
                        showingCreate = true
                    }, onOpenSettings: {
                        selectedTab = 2
                    })
                case 2:
                    SettingsView()
                default:
                    HomeView(showingCreate: $showingCreate, onOpenSettings: { selectedTab = 2 })
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            HStack(spacing: 0) {
                tab(title: "nav.home", icon: "house.fill", index: 0)
                tab(title: "nav.progress", icon: "chart.bar.fill", index: 1)
                tab(title: "nav.profile", icon: "person.fill", index: 2)
            }
            .padding(.horizontal, 14)
            .padding(.top, 7)
            .padding(.bottom, 4)
            .background(AweroDesign.ivory)
            .overlay(alignment: .top) {
                Rectangle().fill(AweroDesign.navy.opacity(0.07)).frame(height: 1)
            }
        }
        .background(AweroDesign.ivory.ignoresSafeArea())
        .tint(AweroDesign.coral)
    }

    private func tab(title: LocalizedStringKey, icon: String, index: Int) -> some View {
        Button { selectedTab = index } label: {
            VStack(spacing: 3) {
                Image(systemName: icon)
                    .font(.system(size: 20, weight: .semibold))
                    .frame(height: 23)
                Text(title)
                    .font(.system(size: 11, weight: .medium, relativeTo: .caption))
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
            }
            .foregroundStyle(selectedTab == index ? AweroDesign.coral : AweroDesign.navy.opacity(0.58))
            .frame(maxWidth: .infinity, minHeight: 44)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("tab.\(index)")
        .accessibilityAddTraits(selectedTab == index ? .isSelected : [])
    }
}
